package com.aicompanion.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务接口
 */
public interface AiChatService {

    /**
     * 同步对话（等待 AI 完整回复后返回）
     */
    String chat(String sessionId, String message);

    /**
     * 流式对话（SSE 打字机效果）
     */
    SseEmitter chatStream(String sessionId, String message);

    /**
     * AI 面试官模式（临时覆盖系统提示词）
     */
    String interview(String sessionId, String message);

    /**
     * 生成知识点（用于技能学习页面）
     */
    String generateKnowledgePoint(String skillName);

    /**
     * 流式考核模式（AI 出题 + 阅卷，覆盖系统提示词）
     */
    SseEmitter examStream(String skillName, String sessionId, String message);
}