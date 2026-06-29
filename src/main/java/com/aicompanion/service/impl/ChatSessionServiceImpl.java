package com.aicompanion.service.impl;

import com.aicompanion.mapper.ChatMessageMapper;
import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.service.ChatSessionService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * AI 会话管理服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ChatSessionServiceImpl implements ChatSessionService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

    @Override
    public Long createSession(Long userId, String title) {
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        session.setTitle(title != null && !title.isBlank() ? title : "新对话");
        session.setAgentType("CHAT");
        chatSessionMapper.insert(session);
        log.info("创建会话: sessionId={}, userId={}, title={}", session.getId(), userId, session.getTitle());
        return session.getId();
    }

    @Override
    public List<ChatSession> listSessions(Long userId) {
        return chatSessionMapper.selectList(
                new LambdaQueryWrapper<ChatSession>()
                        .eq(ChatSession::getUserId, userId)
                        .orderByDesc(ChatSession::getUpdateTime)
        );
    }

    @Override
    public List<ChatMessage> getSessionMessages(Long sessionId) {
        return chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
                        .orderByAsc(ChatMessage::getCreateTime)
        );
    }

    @Override
    @Transactional
    public void deleteSession(Long sessionId, Long userId) {
        // 权限校验：确认会话属于当前用户
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权删除该会话");
        }

        // 逻辑删除关联消息
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );

        // 逻辑删除会话
        chatSessionMapper.deleteById(sessionId);
        log.info("删除会话: sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    @Transactional
    public void clearSessionMessages(Long sessionId, Long userId) {
        // 权限校验
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new RuntimeException("会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new RuntimeException("无权操作该会话");
        }

        // 逻辑删除关联消息
        chatMessageMapper.delete(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, sessionId)
        );
        log.info("清空会话消息: sessionId={}, userId={}", sessionId, userId);
    }

    @Override
    public void updateSessionTitle(Long sessionId, String title) {
        chatSessionMapper.update(null,
                new LambdaUpdateWrapper<ChatSession>()
                        .eq(ChatSession::getId, sessionId)
                        .set(ChatSession::getTitle, title)
        );
    }
}
