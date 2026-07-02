package com.aicompanion.aspect;

import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.service.AiCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.stereotype.Component;

/**
 * AI 调用日志切面
 * 自动拦截所有 AI 服务方法，记录调用日志
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class AiCallLogAspect {

    private final AiCallLogService aiCallLogService;

    /**
     * 切入点：拦截 AiChatServiceImpl 的所有 public 方法
     */
    @Pointcut("execution(public * com.aicompanion.service.impl.AiChatServiceImpl.*(..))")
    public void aiChatServiceMethods() {}

    /**
     * 切入点：拦截 LearningPathServiceImpl 的所有 public 方法
     */
    @Pointcut("execution(public * com.aicompanion.service.impl.LearningPathServiceImpl.*(..))")
    public void learningPathServiceMethods() {}

    /**
     * 环绕通知：记录调用日志
     */
    @Around("aiChatServiceMethods() || learningPathServiceMethods()")
    public Object logAiCall(ProceedingJoinPoint joinPoint) throws Throwable {
        long startTime = System.currentTimeMillis();
        String methodName = joinPoint.getSignature().getName();
        String callType = mapMethodNameToType(methodName);

        // 获取当前用户ID（可能为 null）
        Long userId = getCurrentUserId();

        try {
            // 执行业务方法
            Object result = joinPoint.proceed();

            // 记录成功调用
            long durationMs = System.currentTimeMillis() - startTime;
            aiCallLogService.saveLog(userId, callType, durationMs, 1, null);

            log.debug("AI 调用成功: method={}, userId={}, durationMs={}", methodName, userId, durationMs);

            return result;
        } catch (Exception e) {
            // 记录失败调用
            long durationMs = System.currentTimeMillis() - startTime;
            aiCallLogService.saveLog(userId, callType, durationMs, 0, e.getMessage());

            log.error("AI 调用失败: method={}, userId={}, durationMs={}, error={}",
                    methodName, userId, durationMs, e.getMessage());

            throw e;
        }
    }

    /**
     * 将方法名映射为调用类型
     */
    private String mapMethodNameToType(String methodName) {
        return switch (methodName) {
            case "chat" -> "CHAT";
            case "chatStream" -> "CHAT_STREAM";
            case "interview" -> "INTERVIEW";
            case "generateKnowledgePoint" -> "KNOWLEDGE_POINT";
            case "getRecommendations", "getRecommendationsWithAi" -> "LEARNING_PATH";
            default -> "OTHER";
        };
    }

    /**
     * 获取当前用户ID（未登录时返回 null）
     */
    private Long getCurrentUserId() {
        try {
            return SecurityUtil.getCurrentUserId();
        } catch (Exception e) {
            return null;
        }
    }
}
