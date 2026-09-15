package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.util.ScoreExtractor;
import com.aicompanion.config.DifyConfig;
import com.aicompanion.model.vo.DifyChatResponseVO;
import com.aicompanion.model.vo.DifyGradeResultVO;
import com.aicompanion.model.vo.DifyResumeResponseVO;
import com.aicompanion.service.DifyService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dify AI 平台客户端实现（基于 RestTemplate + HttpClient5 连接池）
 */
@Slf4j
@Service
public class DifyServiceImpl implements DifyService {

    private final DifyConfig difyConfig;
    private final ScoreExtractor scoreExtractor;
    private final RestTemplate restTemplate;

    private static final String EXAM_QUESTION_QUERY = "请为我生成考核题目";
    private static final String GRADE_QUERY = "请根据以上对话内容对我的考核表现进行评分";

    /**
     * 剥离 Dify 思考模型的 <think>...</think> 标签（兼容多种换行和大小写）
     * 鸿蒙端原版位于 ExamViewModel.ets:136
     */
    private static final Pattern THINK_TAG_PATTERN =
            Pattern.compile("<think>[\\s\\S]*?</think>", Pattern.CASE_INSENSITIVE);

    public DifyServiceImpl(DifyConfig difyConfig,
                           ScoreExtractor scoreExtractor,
                           @Qualifier("difyRestTemplate") RestTemplate restTemplate) {
        this.difyConfig = difyConfig;
        this.scoreExtractor = scoreExtractor;
        this.restTemplate = restTemplate;
    }

    // ==================== 公共 API ====================

    @Override
    public DifyChatResponseVO chat(Long userId, String skillName, String query,
                                   String conversation, String conversationId) {
        validateExamKey();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("skill_name", skillName);
        if (conversation != null && !conversation.isBlank()) {
            inputs.put("conversation", conversation);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", buildUser(userId));
        if (conversationId != null && !conversationId.isBlank()) {
            body.put("conversation_id", conversationId);
        }

        return callChatApi(difyConfig.getApps().getExam().getApiKey(), body);
    }

    @Override
    public String generateExamQuestions(Long userId, String skillName) {
        DifyChatResponseVO response = chat(userId, skillName, EXAM_QUESTION_QUERY, null, null);
        String answer = response.getAnswer();
        if (answer == null || answer.isBlank()) {
            throw new BusinessException("AI 出题失败：未收到有效回复，请稍后重试");
        }
        return answer;
    }

    @Override
    public DifyGradeResultVO gradeExam(Long userId, String skillName, String conversation) {
        DifyChatResponseVO response = chat(userId, skillName, GRADE_QUERY, conversation, null);
        String text = response.getAnswer();
        if (text == null || text.isBlank()) {
            throw new BusinessException("AI 阅卷失败：未收到有效评阅文本");
        }
        int score = scoreExtractor.extract(text);
        if (score < 0) {
            log.warn("Dify 阅卷文本无法解析分数: userId={}, skill={}, text长度={}",
                    userId, skillName, text.length());
            throw new BusinessException("AI 阅卷失败：分数格式无法识别，请稍后重试");
        }
        log.info("Dify 阅卷完成: userId={}, skill={}, 提取分数={}", userId, skillName, score);
        return new DifyGradeResultVO(text, score);
    }

    @Override
    public String optimizeResume(Long userId, String resumeContent) {
        validateResumeKey();

        Map<String, Object> inputs = new HashMap<>();
        inputs.put("resume_content", resumeContent);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", buildUser(userId));

        String url = difyConfig.getBaseUrl() + "/workflows/run";
        ResponseEntity<DifyResumeResponseVO> response =
                post(url, difyConfig.getApps().getResume().getApiKey(), body, DifyResumeResponseVO.class);

        if (response.getBody() == null
                || response.getBody().getData() == null
                || response.getBody().getData().getOutputs() == null) {
            log.error("Dify 简历优化返回结构不完整: userId={}, status={}",
                    userId, response.getStatusCode());
            throw new BusinessException("AI 简历优化失败：Dify 返回结构异常");
        }

        String result = response.getBody().getData().getOutputs().getOptimizedResume();
        if (result == null || result.isBlank()) {
            log.warn("Dify 简历优化返回空: userId={}", userId);
            throw new BusinessException("AI 简历优化失败：未收到优化结果");
        }
        log.info("Dify 简历优化完成: userId={}, 输出长度={}", userId, result.length());
        return result;
    }

    // ==================== Dify API 调用 ====================

    /**
     * 调用 Dify /chat-messages API（带 answer 判空 + think 标签剥离）
     */
    private DifyChatResponseVO callChatApi(String apiKey, Map<String, Object> body) {
        String url = difyConfig.getBaseUrl() + "/chat-messages";
        ResponseEntity<DifyChatResponseVO> response =
                post(url, apiKey, body, DifyChatResponseVO.class);

        if (response.getBody() == null) {
            throw new BusinessException("AI 服务暂不可用：Dify 返回空响应");
        }
        if (response.getBody().getAnswer() == null || response.getBody().getAnswer().isBlank()) {
            log.warn("Dify chat-messages 返回 answer 为空: url={}, bodyKeys={}", url, body.keySet());
            throw new BusinessException("AI 服务暂不可用：Dify 未返回有效回复");
        }

        DifyChatResponseVO vo = response.getBody();
        vo.setAnswer(stripThinkTags(vo.getAnswer()));
        return vo;
    }

    /**
     * 通用 POST 请求（区分 4xx / 5xx 异常，补堆栈日志）
     */
    private <T> ResponseEntity<T> post(String url, String apiKey, Map<String, Object> body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        long startTime = System.currentTimeMillis();
        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            long elapsed = System.currentTimeMillis() - startTime;
            log.debug("Dify API 成功: url={}, status={}, elapsed={}ms", url, response.getStatusCode(), elapsed);
            return response;
        } catch (HttpClientErrorException e) {
            // 4xx: API Key 失效、额度用完、参数错误 —— 映射成业务异常
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Dify API 客户端错误: url={}, status={}, elapsed={}ms, error={}",
                    url, e.getStatusCode(), elapsed, e.getStatusText());
            if (e.getStatusCode().isSameCodeAs(HttpStatus.UNAUTHORIZED)) {
                throw new BusinessException(503, "AI 服务鉴权失败：API Key 无效");
            } else if (e.getStatusCode().isSameCodeAs(HttpStatus.TOO_MANY_REQUESTS)) {
                throw new BusinessException(429, "AI 服务调用过于频繁，请稍后再试");
            } else if (e.getStatusCode().isSameCodeAs(HttpStatus.BAD_REQUEST)) {
                throw new BusinessException(400, "AI 服务参数错误，请检查输入");
            } else {
                throw new BusinessException(503, "AI 服务暂不可用（" + e.getStatusCode().value() + "）");
            }
        } catch (RestClientException e) {
            // 网络异常（连接超时、读超时、DNS 等）
            long elapsed = System.currentTimeMillis() - startTime;
            log.error("Dify API 网络异常: url={}, elapsed={}ms, error={}", url, elapsed, e.getMessage(), e);
            throw new BusinessException(503, "AI 服务连接失败，请稍后再试");
        }
    }

    // ==================== 工具方法 ====================

    /**
     * 组装 Dify user 字段：user-{userId}，无 userId 时退化为 anonymous-{threadId}
     * （只有 P0-1 修完后 userId 才一定存在；此处留兜底防万一）
     */
    private String buildUser(Long userId) {
        if (userId != null) {
            return "user-" + userId;
        }
        return "anonymous-" + Thread.currentThread().getId();
    }

    private String stripThinkTags(String text) {
        return THINK_TAG_PATTERN.matcher(text).replaceAll("").trim();
    }

    private void validateExamKey() {
        if (difyConfig.getApps().getExam().getApiKey() == null
                || difyConfig.getApps().getExam().getApiKey().isBlank()) {
            throw new BusinessException(503, "AI 考核服务未配置，请联系管理员");
        }
    }

    private void validateResumeKey() {
        if (difyConfig.getApps().getResume().getApiKey() == null
                || difyConfig.getApps().getResume().getApiKey().isBlank()) {
            throw new BusinessException(503, "AI 简历优化服务未配置，请联系管理员");
        }
    }
}
