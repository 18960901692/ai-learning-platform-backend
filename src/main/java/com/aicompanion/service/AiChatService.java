package com.aicompanion.service;

import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

/**
 * AI 对话服务接口
 */
public interface AiChatService {

    /**
     * 同步对话（等待 AI 完整回复后返回）
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return AI 回复内容
     */
    String chat(String sessionId, String message);

    /**
     * 流式对话（SSE 打字机效果）
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return SseEmitter
     */
    SseEmitter chatStream(String sessionId, String message);

    /**
     * AI 面试官模式（临时覆盖系统提示词）
     * 只问 Java 基础问题、每次只问一题、根据回答追问
     *
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return AI 回复内容
     */
    String interview(String sessionId, String message);
}
