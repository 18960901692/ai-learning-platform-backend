package com.aicompanion.service.impl;

import com.aicompanion.common.util.UserContextHolder;
import com.aicompanion.mapper.ChatMessageMapper;
import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.service.AiChatService;
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
 *
 * <p>ThreadLocal 策略：所有 Tool 的 userId 从 {@link UserContextHolder} 取。
 * 同步方法用 try-finally 防并发覆盖 + 防 ThreadLocal 泄漏；
 * 异步方法（runAsync）在 lambda 内部手动 set 一次复制，因为 ThreadLocal 不跨线程。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiChatServiceImpl implements AiChatService {

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatSessionMapper chatSessionMapper;

    /**
     * 获取当前登录用户ID（未登录时返回 null）
     */
    private Long getCurrentUserId() {
        try {
            return com.aicompanion.common.util.SecurityUtil.getCurrentUserId();
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

        // 存 ThreadLocal —— try-finally 保证无论成功异常都清理，防并发覆盖 + 防泄漏
        UserContextHolder.set(userId);
        try {
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

            // 3. 调用 AI（内部可能触发 Tool，Tool 从 UserContextHolder.get() 取 userId）
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
        } finally {
            UserContextHolder.clear();
        }
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

        // 主线程先 set，但主线程不能一直持有（会造成 ThreadLocal 泄漏），
        // 所以异步线程里手动再 set 一次复制过去，主线程 finally 里清理。
        UserContextHolder.set(userId);
        try {
            // 确保 chat_session 记录存在（在主线程做，需要 RequestContextHolder）
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

            // 异步执行流式调用 —— ThreadLocal 不跨线程，手动复制 userId
            CompletableFuture.runAsync(() -> {
                UserContextHolder.set(userId);  // 关键：把主线程的 userId 复制到异步线程
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
                } finally {
                    UserContextHolder.clear();  // 异步线程里清理，防 ThreadLocal 泄漏
                }
            });
        } finally {
            UserContextHolder.clear();  // 主线程也要清理（即使 runAsync 抛异常也执行）
        }

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

        // 防御性 set/clear —— interview 不走 Tool，但 Tool 单例是共享的，
        // 如果未来 interview 也注册了 Tool，就能正确取到 userId 而不是读到残留值
        UserContextHolder.set(userId);
        try {
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
        } finally {
            UserContextHolder.clear();
        }
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
            log.info("创建面试会话: sessionId={}, userId={}, title={}", sessionId, session.getTitle(), title);
        }
    }

    /**
     * 生成知识点（用于技能学习页面）
     * 每次调用都生成一个不同的知识点，不依赖会话记忆
     */
    @Override
    public String generateKnowledgePoint(String skillName) {
        log.info("生成知识点请求: skillName={}", skillName);

        // 生成随机知识点编号，确保每次调用内容不同
        long seed = System.currentTimeMillis() % 10000;

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
                        6.不要太长,200字以内
                        """)
                .user("请为「" + skillName + "」这个技能生成一个相关的核心知识点（编号：" + seed + "）")
                .call()
                .content();

        log.info("知识点生成完成: skillName={}, replyLength={}", skillName, reply.length());
        return reply;
    }

    /**
     * 流式考核模式（AI 出题 + 阅卷，覆盖系统提示词 + 手动管理 Redis ChatMemory）
     * 使用独立的 exam-{sessionId} 前缀，避免和普通聊天混淆
     */
    @Override
    public SseEmitter examStream(String skillName, String sessionId, String message) {
        log.info("AI 考核流式请求: skillName={}, sessionId={}, message={}", skillName, sessionId, message);

        SseEmitter emitter = new SseEmitter(120000L);
        StringBuilder fullReply = new StringBuilder();

        // 考核会话使用独立的 conversationId
        String examSessionId = "exam-" + sessionId;

        Long userId = getCurrentUserId();

        // 主线程读历史（需要 RequestContextHolder，如果依赖的话）
        List<Message> history = chatMemory.get(examSessionId);
        log.info("从 Redis 读取考核历史消息: sessionId={}, count={}", examSessionId, history.size());

        List<Message> messages = new ArrayList<>(history);
        messages.add(new UserMessage(message));

        // 异步执行流式调用 —— 手动复制 ThreadLocal
        CompletableFuture.runAsync(() -> {
            UserContextHolder.set(userId);
            try {
                String systemPrompt = String.format("""
                        你是一位严格的专业技能考核官，正在对考生进行技能评估。
                        
                        考核技能：%s
                        
                        规则：
                        1. 你只负责考核「%s」相关的知识和技能
                        2. 每次只出一道题，不要一次出多个问题
                        3. 题目类型包含：选择题、判断题、填空题、简答题
                        4. 第一题请从基础知识开始，之后根据回答情况调整难度
                        5. 回答正确时给予肯定，错误时给出提示并允许重新回答
                        6. 每道题考生回答后，先评判对错，再给出正确答案和解析
                        7. 出满 5 道题后，询问考生是否完成考核
                        8. 如果考生回答"完成"、"结束"、"提交"或类似词语，请给出最终评分报告
                        
                        评分报告格式要求（必须严格按照以下格式输出）：
                        ## 考核报告
                        
                        **总分：X分 / 100分**
                        
                        ### 答题详情
                        1. 第1题：[题目] → [是否正确] → [得分]
                        2. 第2题：[题目] → [是否正确] → [得分]
                        3. 第3题：[题目] → [是否正确] → [得分]
                        4. 第4题：[题目] → [是否正确] → [得分]
                        5. 第5题：[题目] → [是否正确] → [得分]
                        
                        ### 综合评价
                        [对考生整体表现的简短评价]
                        
                        ### 学习建议
                        [给出针对性的学习建议]
                        
                        评分规则：
                        - 每题 20 分，共 5 题，满分 100 分
                        - 选择题/判断题答对得满分，答错 0 分
                        - 简答题根据回答完整度和准确性给 0-20 分
                        
                        注意：在最终给出评分报告之前，不要透露评分规则和分数。
                        语气专业友好，适当给予鼓励。
                        """, skillName, skillName);
                chatClient.prompt()
                        .system(systemPrompt)
                        .messages(messages)
                        .stream()
                        .content()
                        .doOnNext(chunk -> {
                            try {
                                fullReply.append(chunk);
                                Map<String, String> event = new LinkedHashMap<>();
                                event.put("event", "message");
                                event.put("data", chunk);
                                emitter.send(SseEmitter.event().data(event));
                            } catch (IOException e) {
                                log.error("考核 SSE 发送失败", e);
                                emitter.completeWithError(e);
                            }
                        })
                        .doOnComplete(() -> {
                            try {
                                Map<String, String> doneEvent = new LinkedHashMap<>();
                                doneEvent.put("event", "done");
                                emitter.send(SseEmitter.event().data(doneEvent));
                            } catch (IOException e) {
                                log.error("考核 SSE 发送完成事件失败", e);
                            }
                            // 保存到 Redis
                            List<Message> toSave = new ArrayList<>();
                            toSave.add(new UserMessage(message));
                            toSave.add(new AssistantMessage(fullReply.toString()));
                            chatMemory.add(examSessionId, toSave);

                            emitter.complete();
                            log.info("AI 考核流式完成: skillName={}, sessionId={}, replyLength={}",
                                    skillName, sessionId, fullReply.length());
                        })
                        .doOnError(error -> {
                            try {
                                Map<String, String> errorEvent = new LinkedHashMap<>();
                                errorEvent.put("event", "error");
                                errorEvent.put("data", error.getMessage());
                                emitter.send(SseEmitter.event().data(errorEvent));
                            } catch (IOException e) {
                                log.error("考核 SSE 发送错误事件失败", e);
                            }
                            log.error("AI 考核流式出错: sessionId={}", sessionId, error);
                            emitter.completeWithError(error);
                        })
                        .subscribe();
            } catch (Exception e) {
                log.error("AI 考核流式异常: sessionId={}", sessionId, e);
                emitter.completeWithError(e);
            } finally {
                UserContextHolder.clear();
            }
        });

        return emitter;
    }
}
