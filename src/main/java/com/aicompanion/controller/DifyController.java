package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.DifyChatRequestDTO;
import com.aicompanion.model.dto.DifyResumeRequestDTO;
import com.aicompanion.model.vo.DifyChatResponseVO;
import com.aicompanion.model.vo.DifyGradeResultVO;
import com.aicompanion.service.DifyService;
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
 */
@Tag(name = "Dify AI 代理", description = "代理鸿蒙端调用 Dify AI 平台的对话、出题、阅卷、简历优化")
@RestController
@RequestMapping("/dify")
@RequiredArgsConstructor
@Slf4j
public class DifyController {

    private final DifyService difyService;
    private final ObjectMapper objectMapper;

    /**
     * 考核多轮对话
     */
    @Operation(summary = "考核对话", description = "多轮考核对话，自动透传 conversation_id 保持上下文")
    @PostMapping("/exam/chat")
    public Result<DifyChatResponseVO> examChat(@Valid @RequestBody DifyChatRequestDTO request) {
        String query = request.getQuery();
        if (query == null || query.isBlank()) {
            query = "你好，请开始考核";
        }
        DifyChatResponseVO response = difyService.chat(
                request.getSkillName(),
                query,
                null,
                request.getConversationId()
        );
        return Result.success(response);
    }

    /**
     * 生成考核题目
     */
    @Operation(summary = "生成考核题目", description = "让 Dify 根据技能名称生成考核题目列表")
    @PostMapping("/exam/questions")
    public Result<List<Map<String, Object>>> generateQuestions(@Valid @RequestBody DifyChatRequestDTO request) {
        String answer = difyService.generateExamQuestions(request.getSkillName());
        try {
            // Dify answer 应该是 JSON 数组字符串，如 [{"id":1,"type":"SINGLE","question":"..."}]
            List<Map<String, Object>> questions = objectMapper.readValue(answer, new TypeReference<>() {});
            return Result.success(questions);
        } catch (Exception e) {
            log.error("解析 Dify 出题结果失败: {}", e.getMessage());
            // Dify 输出格式不是 JSON 数组时，返回原文（方便排查）
            Map<String, Object> raw = Map.of("raw_answer", answer);
            return Result.success(List.of(raw));
        }
    }

    /**
     * 阅卷评分
     */
    @Operation(summary = "阅卷评分", description = "让 Dify 根据完整对话记录对考生表现进行评分")
    @PostMapping("/exam/grade")
    public Result<DifyGradeResultVO> examGrade(@Valid @RequestBody DifyChatRequestDTO request) {
        DifyGradeResultVO result = difyService.gradeExam(
                request.getSkillName(),
                request.getConversation()
        );
        return Result.success(result);
    }

    /**
     * 简历优化
     */
    @Operation(summary = "简历优化", description = "调用 Dify Workflow 对简历进行优化")
    @PostMapping("/resume/optimize")
    public Result<String> optimizeResume(@Valid @RequestBody DifyResumeRequestDTO request) {
        String optimized = difyService.optimizeResume(request.getResumeContent());
        return Result.success(optimized);
    }
}
