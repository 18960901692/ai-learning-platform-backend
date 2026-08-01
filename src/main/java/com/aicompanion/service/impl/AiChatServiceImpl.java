package com.aicompanion.service.impl;

import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.ChatMessageMapper;
import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.service.AiChatService;
import com.aicompanion.tool.LearningRecordTool;
import com.aicompanion.tool.SkillLookupTool;
import com.aicompanion.tool.UserSkillAnalysisTool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

/**
 * AI 对话服务实现类（基于 Spring AI ChatClient + Redis ChatMemory 多轮对话）
 *
 * <p>手动管理 ChatMemory：发送前 get() 读取历史，回复后 add() 保存上下文。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final SkillLookupTool skillLookupTool;
    private final UserSkillAnalysisTool userSkillAnalysisTool;
    private final LearningRecordTool learningRecordTool;

    /**
     * 获取当前登录用户ID（未登录时返回 null）
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 解析 sessionId：提取数字部分
     */
    private Long parseSessionId(String sessionId) {
        try {
            String digits = sessionId.replaceAll("[^0-9]", "");
            return digits.isEmpty() ? null : Long.parseLong(digits);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 确保 chat_session 记录存在（首次对话时自动创建）
     */
    private void ensureSessionExists(Long sessionId, Long userId, String firstMessage) {
        ChatSession existing = chatSessionMapper.selectById(sessionId);
        if (existing == null) {
            String title = firstMessage != null && firstMessage.length() > 20
                    ? firstMessage.substring(0, 20) : firstMessage;
            ChatSession session = new ChatSession();
            session.setId(sessionId);
            session.setUserId(userId);
            session.setTitle(title != null && !title.isBlank() ? title : "新对话");
            session.setAgentType("CHAT");
            chatSessionMapper.insert(session);
            log.info("自动创建会话记录: sessionId={}, title={}", sessionId, session.getTitle());
        }
    }

    /**
     * 保存用户消息到数据库
     */
    private void saveUserMessage(Long sessionId, Long userId, String message) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole("user");
        msg.setContent(message);
        chatMessageMapper.insert(msg);
    }

    /**
     * 保存 AI 回复到数据库
     */
    private void saveAssistantMessage(Long sessionId, Long userId, String reply) {
        ChatMessage msg = new ChatMessage();
        msg.setSessionId(sessionId);
        msg.setUserId(userId);
        msg.setRole("assistant");
        msg.setContent(reply);
        chatMessageMapper.insert(msg);
    }

    /**
     * 同步对话（手动管理 Redis ChatMemory）
     */
    @Override
    public String chat(String sessionId, String message) {
        log.info("AI 对话请求: sessionId={}, message={}", sessionId, message);

        Long userId = getCurrentUserId();

        // 设置用户ID到工具对象
        skillLookupTool.setCurrentUserId(userId);
        userSkillAnalysisTool.setCurrentUserId(userId);
        learningRecordTool.setCurrentUserId(userId);

        // 确保 chat_session 记录存在
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId != null) {
            ensureSessionExists(dbSessionId, userId, message);
            saveUserMessage(dbSessionId, userId, message);
        }

        // 1. 从 Redis 读取历史对话
        List<Message> history = chatMemory.get(sessionId);
        log.info("从 Redis 读取历史消息: sessionId={}, count={}", sessionId, history.size());

        // 2. 构建消息列表：历史 + 当前用户消息
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(message));

        // 3. 调用 AI
        String reply = chatClient.prompt(new Prompt(messages))
                .call()
                .content();

        // 4. 保存对话到 Redis（用户消息 + AI回复）
        List<Message> toSave = new ArrayList<>();
        toSave.add(new UserMessage(message));
        toSave.add(new AssistantMessage(reply));
        chatMemory.add(sessionId, toSave);

        log.info("AI 对话回复: sessionId={}, replyLength={}", sessionId, reply.length());

        // 5. 保存 AI 回复到数据库
        if (dbSessionId != null) {
            saveAssistantMessage(dbSessionId, userId, reply);
        }
        return reply;
    }

    /**
     * 流式对话（SSE + 手动管理 Redis ChatMemory）
     */
    @Override
    public SseEmitter chatStream(String sessionId, String message) {
        log.info("AI 流式对话请求: sessionId={}, message={}", sessionId, message);

        SseEmitter emitter = new SseEmitter(120000L);
        StringBuilder fullReply = new StringBuilder();

        Long userId = getCurrentUserId();

        // 设置用户ID到工具对象
        skillLookupTool.setCurrentUserId(userId);
        userSkillAnalysisTool.setCurrentUserId(userId);
        learningRecordTool.setCurrentUserId(userId);

        // 确保 chat_session 记录存在
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId != null) {
            ensureSessionExists(dbSessionId, userId, message);
            saveUserMessage(dbSessionId, userId, message);
        }

        // 从 Redis 读取历史对话
        List<Message> history = chatMemory.get(sessionId);
        log.info("从 Redis 读取历史消息: sessionId={}, count={}", sessionId, history.size());

        // 构建消息列表：历史 + 当前用户消息
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(message));

        // 异步执行流式调用
        CompletableFuture.runAsync(() -> {
            try {
                chatClient.prompt(new Prompt(messages))
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            try {
                                fullReply.append(chunk);
                                Map<String, String> event = new LinkedHashMap<>();
                                event.put("event", "message");
                                event.put("data", chunk);
                                // 直接传 Map，让 Spring 的 data() 序列化一次即可
                                // 之前用 mapper.writeValueAsString(event) 会导致 String 被二次序列化
                                emitter.send(SseEmitter.event().data(event));
                            } catch (IOException e) {
                                log.error("SSE 发送失败", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                Map<String, String> doneEvent = new LinkedHashMap<>();
                                doneEvent.put("event", "done");
                                emitter.send(SseEmitter.event().data(doneEvent));
                            } catch (IOException e) {
                                log.error("SSE 发送完成事件失败", e);
                            }
                            // 保存到 Redis
                            List<Message> toSave = new ArrayList<>();
                            toSave.add(new UserMessage(message));
                            toSave.add(new AssistantMessage(fullReply.toString()));
                            chatMemory.add(sessionId, toSave);

                            if (dbSessionId != null) {
                                saveAssistantMessage(dbSessionId, userId, fullReply.toString());
                            }
                            emitter.complete();
                            log.info("AI 流式对话完成: sessionId={}, replyLength={}", sessionId, fullReply.length());
                        })
                        .doOnError(error -> {
                            try {
                                Map<String, String> errorEvent = new LinkedHashMap<>();
                                errorEvent.put("event", "error");
                                errorEvent.put("data", error.getMessage());
                                emitter.send(SseEmitter.event().data(errorEvent));
                            } catch (IOException e) {
                                log.error("SSE 发送错误事件失败", e);
                            }
                            log.error("AI 流式对话出错: sessionId={}", sessionId, error);
                            emitter.completeWithError(error);
                        })
                        .subscribe();
            } catch (Exception e) {
                log.error("AI 流式对话异常: sessionId={}", sessionId, e);
                emitter.completeWithError(e);
            }
        });

        return emitter;
    }

    /**
     * AI 面试官模式（临时覆盖系统提示词 + 手动管理 Redis ChatMemory）
     * 只问 Java 基础问题、每次只问一题、根据回答追问
     * 会话记录保存到数据库，agent_type=INTERVIEW
     */
    @Override
    public String interview(String sessionId, String message) {
        log.info("AI 面试官请求: sessionId={}, message={}", sessionId, message);

        // 面试会话使用独立的 conversationId，避免和普通聊天混淆
        String interviewSessionId = "interview-" + sessionId;

        Long userId = getCurrentUserId();

        // 确保 chat_session 记录存在（agent_type=INTERVIEW）
        Long dbSessionId = parseSessionId(sessionId);
        if (dbSessionId != null) {
            ensureInterviewSessionExists(dbSessionId, userId, message);
            saveUserMessage(dbSessionId, userId, message);
        }

        // 从 Redis 读取历史对话
        List<Message> history = chatMemory.get(interviewSessionId);
        log.info("从 Redis 读取面试历史消息: sessionId={}, count={}", interviewSessionId, history.size());

        // 构建消息列表：历史 + 当前用户消息
        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(message));

        // 调用 AI
        String reply = chatClient.prompt()
                .system("""
                        你是一位严格的全栈技术面试官，正在对候选人进行面试。
                        规则：
                        1. 考察范围覆盖全栈技术栈，包括但不限于：
                           - 后端：Java 基础（面向对象、集合框架、异常处理、多线程、JVM 基础等）、Spring 框架、数据库（MySQL、Redis）、接口设计
                           - 前端：HTML/CSS/JS 基础、Vue 或 React 框架、HTTP 协议、前端工程化
                           - 工程实践：版本控制、构建工具、部署运维、系统设计
                        2. 每次只问一题，不要一次问多个问题
                        3. 根据候选人的回答进行追问：回答得好就深入追问，回答不好就换一个问题或换一个方向
                        4. 知识点之间可适当切换，但同一话题至少追问 1-2 个层次，避免走马观花
                        5. 语气专业但不失友好，适当给予鼓励
                        6. 如果候选人回答"开始"或"准备好了"，直接出第一道题
                        7. 回答要简洁，不要长篇大论
                        8. 不要主动结束面试，除非候选人说"结束"或"不想面了",那么请复盘本次全部面试对话，对面试者进行全面评估。依次输出：
                        综合评价：整体沟通状态、逻辑思维、临场发挥；
                        技术能力：后端、前端、数据库、工程实践等知识点掌握程度；
                        优点总结：回答出彩的地方、个人优势；
                        待提升项：知识盲区、表述问题、思路不足，并给出具体学习建议；
                        最终结论：给出评级以及是否推荐录用的意见。
                        """)
                .messages(messages)
                .call()
                .content();

        // 保存到 Redis
        List<Message> toSave = new ArrayList<>();
        toSave.add(new UserMessage(message));
        toSave.add(new AssistantMessage(reply));
        chatMemory.add(interviewSessionId, toSave);

        log.info("AI 面试官回复: sessionId={}, replyLength={}", sessionId, reply.length());

        if (dbSessionId != null) {
            saveAssistantMessage(dbSessionId, userId, reply);
        }
        return reply;
    }

    /**
     * 确保面试会话记录存在（agent_type=INTERVIEW）
     */
    private void ensureInterviewSessionExists(Long sessionId, Long userId, String firstMessage) {
        ChatSession session = chatSessionMapper.selectById(sessionId);
        if (session == null) {
            String title = firstMessage != null && firstMessage.length() > 20
                    ? firstMessage.substring(0, 20) : firstMessage;
            session = new ChatSession();
            session.setId(sessionId);
            session.setUserId(userId);
            session.setTitle(title != null && !title.isBlank() ? title : "面试对话");
            session.setAgentType("INTERVIEW");
            chatSessionMapper.insert(session);
            log.info("创建面试会话: sessionId={}, userId={}, title={}", sessionId, userId, session.getTitle());
        }
    }

    /**
     * 生成知识点（用于技能学习页面）
     * 每次调用都生成一个不同的知识点，不依赖会话记忆
     */
    @Override
    public String generateKnowledgePoint(String skillName) {
        log.info("生成知识点请求: skillName={}", skillName);

        String reply = chatClient.prompt()
                .system("""
                        你是一位专业的编程知识导师。请根据用户指定的技能，生成一个核心知识点。
                        规则：
                        1. 每次只输出一个知识点，不要列出多个
                        2. 知识点要实用、有深度，适合学习
                        3. 输出格式：
                           【知识点标题】
                           简要说明（1-2句话）
                           核心要点（3-5条，用数字编号）
                           示例代码（如有必要，用代码块包裹）
                           学习建议（1句话）
                        4. 每次调用时生成不同的知识点，不要重复
                        5. 知识点必须与用户指定的技能直接相关
                        """)
                .user("请为「" + skillName + "」这个技能生成一个相关的核心知识点")
                .call()
                .content();

        log.info("知识点生成完成: skillName={}, replyLength={}", skillName, reply.length());
        return reply;
    }
}
