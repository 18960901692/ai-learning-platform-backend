package com.aicompanion.service;

import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;

import java.util.List;

/**
 * AI 会话管理服务接口
 */
public interface ChatSessionService {

    /**
     * 创建新会话
     *
     * @param userId 用户ID
     * @param title  会话标题
     * @return 会话ID
     */
    Long createSession(Long userId, String title);

    /**
     * 获取用户的会话列表（按更新时间倒序）
     *
     * @param userId 用户ID
     * @return 会话列表
     */
    List<ChatSession> listSessions(Long userId);

    /**
     * 获取指定会话的消息历史
     *
     * @param sessionId 会话ID
     * @return 消息列表
     */
    List<ChatMessage> getSessionMessages(Long sessionId);

    /**
     * 删除会话（同时删除关联消息）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（权限校验）
     */
    void deleteSession(Long sessionId, Long userId);

    /**
     * 清空指定会话的消息（保留会话记录）
     *
     * @param sessionId 会话ID
     * @param userId    用户ID（权限校验）
     */
    void clearSessionMessages(Long sessionId, Long userId);

    /**
     * 更新会话标题
     *
     * @param sessionId 会话ID
     * @param title     新标题
     */
    void updateSessionTitle(Long sessionId, String title);
}
