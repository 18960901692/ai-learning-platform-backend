package com.aicompanion.controller;

import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.service.ChatSessionService;
import com.aicompanion.common.response.Result;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * AI 会话管理 Controller
 */
@Slf4j
@RestController
@RequestMapping("/ai/chat/session")
@RequiredArgsConstructor
public class ChatSessionController {

    private final ChatSessionService chatSessionService;

    /**
     * 创建新会话
     */
    @PostMapping
    public Result<Long> createSession(@RequestParam(defaultValue = "新对话") String title) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long sessionId = chatSessionService.createSession(userId, title);
        return Result.success(sessionId);
    }

    /**
     * 获取用户的历史会话列表（支持按 agentType 筛选）
     */
    @GetMapping("/list")
    public Result<List<ChatSession>> listSessions(@RequestParam(required = false) String agentType) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<ChatSession> sessions = chatSessionService.listSessions(userId, agentType);
        return Result.success(sessions);
    }

    /**
     * 获取指定会话的消息历史
     */
    @GetMapping("/{sessionId}/messages")
    public Result<List<ChatMessage>> getSessionMessages(@PathVariable Long sessionId) {
        List<ChatMessage> messages = chatSessionService.getSessionMessages(sessionId);
        return Result.success(messages);
    }

    /**
     * 删除会话
     */
    @DeleteMapping("/{sessionId}")
    public Result<Void> deleteSession(@PathVariable Long sessionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        chatSessionService.deleteSession(sessionId, userId);
        return Result.success();
    }

    /**
     * 清空指定会话的消息（保留会话记录）
     */
    @DeleteMapping("/{sessionId}/messages")
    public Result<Void> clearSessionMessages(@PathVariable Long sessionId) {
        Long userId = SecurityUtil.getCurrentUserId();
        chatSessionService.clearSessionMessages(sessionId, userId);
        return Result.success();
    }

    /**
     * 更新会话标题
     */
    @PutMapping("/{sessionId}/title")
    public Result<Void> updateSessionTitle(@PathVariable Long sessionId, @RequestParam String title) {
        chatSessionService.updateSessionTitle(sessionId, title);
        return Result.success();
    }
}
