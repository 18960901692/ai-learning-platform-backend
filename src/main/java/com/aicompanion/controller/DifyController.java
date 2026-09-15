package com.aicompanion.controller;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.DifyChatRequestDTO;
import com.aicompanion.model.dto.DifyResumeRequestDTO;
import com.aicompanion.model.vo.DifyChatResponseVO;
import com.aicompanion.model.vo.DifyGradeResultVO;
import com.aicompanion.model.vo.ExamSessionVO;
import com.aicompanion.service.DifyService;
import com.aicompanion.service.ExamService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * Dify AI 平台代理接口
 * 替代鸿蒙端直连 Dify 的硬编码调用，统一在后端管理 API Key
 *
 * 所有接口强制登录（JwtInterceptor 已放行规则已移除），
 * userId 从 SecurityUtil 获取后透传到 Dify Service 隔离 Dify 侧会话归属。
 */
@Tag(name = "Dify AI 代理", description = "代理鸿蒙端调用 Dify AI 平台的对话、出题、阅卷、简历优化（需登录）")
@RestController
@RequestMapping("/dify")
@RequiredArgsConstructor
@Slf4j
public class DifyController {

    private final DifyService difyService;
    private final ExamService examService;
    private final ObjectMapper objectMapper;

    /**
     * 考核多轮对话
     */
    @Operation(summary = "考核对话", description = "多轮考核对话，自动透传 conversation_id 保持上下文")
    @PostMapping("/exam/chat")
    public Result<DifyChatResponseVO> examChat(@Valid @RequestBody DifyChatRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            query = "你好，请开始考核";
        }
        DifyChatResponseVO response = difyService.chat(
                userId, request.getSkillName(), query,
                null, request.getConversationId());
        return Result.success(response);
    }

    /**
     * 生成考核题目
     */
    @Operation(summary = "生成考核题目", description = "让 Dify 根据技能名称生成考核题目列表")
    @PostMapping("/exam/questions")
    public Result<List<Map<String, Object>>> generateQuestions(@Valid @RequestBody DifyChatRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String answer = difyService.generateExamQuestions(userId, request.getSkillName());

        try {
            // Dify answer 应该是 JSON 数组字符串
            List<Map<String, Object>> questions = objectMapper.readValue(answer, new TypeReference<>() {});
            if (questions == null || questions.isEmpty()) {
                throw new BusinessException("AI 出题失败：返回题目为空");
            }
            return Result.success(questions);
        } catch (BusinessException be) {
            throw be;
        } catch (Exception e) {
            log.error("解析 Dify 出题结果失败: skill={}, answer前200={}",
                    request.getSkillName(),
                    answer.length() > 200 ? answer.substring(0, 200) : answer);
            throw new BusinessException("AI 出题失败：返回格式异常，请稍后重试");
        }
    }

    /**
     * 阅卷评分（后端闭环：算分 + 直接落库到 ExamSession）
     *
     * 请求体需要带 sessionId —— 告诉后端把分数写到哪条考核会话上
     */
    @Operation(summary = "阅卷评分", description = "让 Dify 阅卷 + 自动落库到考核会话，不再需要客户端二次存分")
    @PostMapping("/exam/grade")
    public Result<ExamSessionVO> examGrade(@Valid @RequestBody DifyChatRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();

        // 必须传 sessionId 才能落库
        String conversationId = request.getConversationId();
        if (conversationId == null || conversationId.isBlank()) {
            throw new BusinessException("阅卷请求缺少 sessionId");
        }
        Long sessionId;
        try {
            sessionId = Long.parseLong(conversationId);
        } catch (NumberFormatException e) {
            throw new BusinessException("sessionId 格式不正确");
        }

        // Dify 阅卷
        DifyGradeResultVO result = difyService.gradeExam(
                userId, request.getSkillName(), request.getConversation());

        // 直接落库 —— 分数来自后端计算，客户端无法伪造
        ExamSessionVO saved = examService.saveDifyGrade(userId, sessionId, result.getText(), result.getScore());
        log.info("阅卷完成并落库: userId={}, sessionId={}, score={}, passed={}",
                userId, sessionId, result.getScore(), saved.getPassed());
        return Result.success(saved);
    }

    /**
     * 简历优化
     */
    @Operation(summary = "简历优化", description = "调用 Dify Workflow 对简历进行优化")
    @PostMapping("/resume/optimize")
    public Result<String> optimizeResume(@Valid @RequestBody DifyResumeRequestDTO request) {
        Long userId = SecurityUtil.getCurrentUserId();
        String optimized = difyService.optimizeResume(userId, request.getResumeContent());
        return Result.success(optimized);
    }
}
