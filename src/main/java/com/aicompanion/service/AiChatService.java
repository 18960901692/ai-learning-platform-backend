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

    /**
     * 生成知识点（用于技能学习页面）
     * 根据技能名称生成一个核心知识点，包含知识点标题和详细内容
     *
     * @param skillName 技能名称
     * @return 知识点内容
     */
    String generateKnowledgePoint(String skillName);

    /**
     * 流式考核模式（AI 出题 + 阅卷，覆盖系统提示词）
     *
     * @param skillName 技能名称
     * @param sessionId 会话 ID
     * @param message   用户消息
     * @return SseEmitter
     */
    SseEmitter examStream(String skillName, String sessionId, String message);
}
