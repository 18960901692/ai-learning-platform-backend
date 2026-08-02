package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.AiChatRequestDTO;
import com.aicompanion.model.dto.ExamChatRequestDTO;
import com.aicompanion.model.dto.KnowledgePointRequestDTO;
import com.aicompanion.service.AiChatService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话控制器（基于 Spring AI）
 */
@Tag(name = "AI 对话", description = "AI 智能问答相关接口（同步 + 流式）")
@RestController
@RequestMapping("/ai/chat")
@RequiredArgsConstructor
public class AiChatController {

    private final AiChatService aiChatService;

    /**
     * 同步对话接口
     */
    @Operation(summary = "同步对话", description = "发送消息，等待 AI 完整回复后返回")
    @PostMapping("/message")
    public Result<String> chat(@Valid @RequestBody AiChatRequestDTO request) {
        String reply = aiChatService.chat(request.getSessionId(), request.getMessage());
        return Result.success(reply);
    }

    /**
     * 流式对话接口（SSE 打字机效果）
     */
    @Operation(summary = "流式对话", description = "发送消息，AI 逐字返回（打字机效果）")
    @PostMapping("/stream")
    public SseEmitter chatStream(@Valid @RequestBody AiChatRequestDTO request) {
        return aiChatService.chatStream(request.getSessionId(), request.getMessage());
    }

    /**
     * AI 面试官模式（课后练习：用 .system() 临时覆盖系统提示词）
     */
    @Operation(summary = "AI 面试官", description = "Java 技术面试模拟，每次只问一题，根据回答追问")
    @PostMapping("/interview")
    public Result<String> interview(@Valid @RequestBody AiChatRequestDTO request) {
        String reply = aiChatService.interview(request.getSessionId(), request.getMessage());
        return Result.success(reply);
    }

    /**
     * 生成知识点（用于技能学习页面）
     */
    @Operation(summary = "生成知识点", description = "根据技能名称生成一个核心知识点")
    @PostMapping("/knowledge-point")
    public Result<String> generateKnowledgePoint(@Valid @RequestBody KnowledgePointRequestDTO request) {
        String reply = aiChatService.generateKnowledgePoint(request.getSkillName());
        return Result.success(reply);
    }

    /**
     * 流式考核模式（AI 出题 + 阅卷）
     */
    @Operation(summary = "流式考核", description = "AI 考核官出题 + 阅卷（打字机效果）")
    @PostMapping("/exam/stream")
    public SseEmitter examStream(@Valid @RequestBody ExamChatRequestDTO request) {
        return aiChatService.examStream(request.getSkillName(), request.getSessionId(), request.getMessage());
    }
}
