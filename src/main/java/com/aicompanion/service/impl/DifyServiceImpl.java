package com.aicompanion.service.impl;

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
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Dify AI 平台客户端实现（基于 RestTemplate）
 */
@Slf4j
@Service
public class DifyServiceImpl implements DifyService {

    private final DifyConfig difyConfig;
    private final ScoreExtractor scoreExtractor;
    private final RestTemplate restTemplate;

    private static final String USER_ID = "backend-api";
    private static final String EXAM_QUESTION_QUERY = "请为我生成考核题目";
    private static final String GRADE_QUERY = "请根据以上对话内容对我的考核表现进行评分";

    /**
     * 剥离 Dify 思考模型的 <think>...</think> 标签
     * 鸿蒙端原版位于 ExamViewModel.ets:136
     */
    private static final Pattern THINK_TAG_PATTERN = Pattern.compile("<think>[\\s\\S]*?</think>", Pattern.CASE_INSENSITIVE);

    public DifyServiceImpl(DifyConfig difyConfig,
                           ScoreExtractor scoreExtractor,
                           @Qualifier("difyRestTemplate") RestTemplate restTemplate) {
        this.difyConfig = difyConfig;
        this.scoreExtractor = scoreExtractor;
        this.restTemplate = restTemplate;
    }

    @Override
    public DifyChatResponseVO chat(String skillName, String query, String conversation, String conversationId) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("skill_name", skillName);
        if (conversation != null && !conversation.isBlank()) {
            inputs.put("conversation", conversation);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("query", query);
        body.put("response_mode", "blocking");
        body.put("user", USER_ID);
        if (conversationId != null && !conversationId.isBlank()) {
            body.put("conversation_id", conversationId);
        }

        return callChatApi(difyConfig.getApps().getExam().getApiKey(), body);
    }

    @Override
    public String generateExamQuestions(String skillName) {
        DifyChatResponseVO response = chat(skillName, EXAM_QUESTION_QUERY, null, null);
        return response.getAnswer();
    }

    @Override
    public DifyGradeResultVO gradeExam(String skillName, String conversation) {
        DifyChatResponseVO response = chat(skillName, GRADE_QUERY, conversation, null);
        String text = response.getAnswer();
        int score = scoreExtractor.extract(text);
        log.info("Dify 阅卷完成: skill={}, text长度={}, 提取分数={}", skillName, text.length(), score);
        return new DifyGradeResultVO(text, score);
    }

    @Override
    public String optimizeResume(String resumeContent) {
        Map<String, Object> inputs = new HashMap<>();
        inputs.put("resume_content", resumeContent);

        Map<String, Object> body = new HashMap<>();
        body.put("inputs", inputs);
        body.put("response_mode", "blocking");
        body.put("user", USER_ID);

        String url = difyConfig.getBaseUrl() + "/workflows/run";
        String apiKey = difyConfig.getApps().getResume().getApiKey();

        ResponseEntity<DifyResumeResponseVO> response = post(url, apiKey, body, DifyResumeResponseVO.class);
        if (response.getBody() != null
                && response.getBody().getData() != null
                && response.getBody().getData().getOutputs() != null) {
            String result = response.getBody().getData().getOutputs().getOptimizedResume();
            log.info("Dify 简历优化完成: 输出长度={}", result != null ? result.length() : 0);
            return result != null ? result : "";
        }
        log.warn("Dify 简历优化返回结构不完整");
        return "";
    }

    /**
     * 调用 Dify /chat-messages API
     */
    private DifyChatResponseVO callChatApi(String apiKey, Map<String, Object> body) {
        String url = difyConfig.getBaseUrl() + "/chat-messages";
        ResponseEntity<DifyChatResponseVO> response = post(url, apiKey, body, DifyChatResponseVO.class);
        if (response.getBody() == null) {
            throw new RuntimeException("Dify chat API 返回为空");
        }
        // 剥离思考模型的 <think>...</think> 标签
        DifyChatResponseVO vo = response.getBody();
        if (vo.getAnswer() != null) {
            vo.setAnswer(stripThinkTags(vo.getAnswer()));
        }
        return vo;
    }

    /**
     * 剥离 <think>...</think> 标签（兼容多种换行和大小写）
     */
    private String stripThinkTags(String text) {
        return THINK_TAG_PATTERN.matcher(text).replaceAll("").trim();
    }

    /**
     * 通用 POST 请求封装（带 Bearer Token 认证）
     */
    private <T> ResponseEntity<T> post(String url, String apiKey, Map<String, Object> body, Class<T> responseType) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(body, headers);

        log.debug("调用 Dify API: url={}, bodyKeys={}", url, body.keySet());

        try {
            ResponseEntity<T> response = restTemplate.exchange(url, HttpMethod.POST, entity, responseType);
            log.debug("Dify API 响应: status={}", response.getStatusCode());
            return response;
        } catch (Exception e) {
            log.error("Dify API 调用失败: url={}, error={}", url, e.getMessage());
            throw new RuntimeException("Dify API 调用失败: " + e.getMessage(), e);
        }
    }
}
