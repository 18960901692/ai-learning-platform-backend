package com.aicompanion.common.util;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 安全工具类
 */
public class SecurityUtil {

    private static final String USER_ID_ATTR = "userId";
    private static final String USER_TYPE_ATTR = "userType";
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * 从 Request 中获取当前登录用户ID
     */
    public static Long getCurrentUserId(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTR);
        if (userId == null) {
            throw new RuntimeException("用户未登录");
        }
        return (Long) userId;
    }

    /**
     * 从当前线程上下文中获取用户ID（无需传入 request）
     */
    public static Long getCurrentUserId() {
        ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attrs == null) {
            throw new RuntimeException("无法获取当前请求上下文");
        }
        return getCurrentUserId(attrs.getRequest());
    }

    /**
     * 从 Request 中获取当前用户类型（USER / ADMIN）
     */
    public static String getCurrentUserType(HttpServletRequest request) {
        Object userType = request.getAttribute(USER_TYPE_ATTR);
        return userType != null ? (String) userType : "USER";
    }

    /**
     * 将用户信息存入 Request
     */
    public static void setCurrentUser(HttpServletRequest request, Long userId, String userType) {
        request.setAttribute(USER_ID_ATTR, userId);
        request.setAttribute(USER_TYPE_ATTR, userType);
    }

    /**
     * 从请求头中提取 Token
     */
    public static String extractToken(HttpServletRequest request) {
        String token = request.getHeader(AUTHORIZATION_HEADER);
        if (token != null && token.startsWith(BEARER_PREFIX)) {
            return token.substring(BEARER_PREFIX.length());
        }
        return token;
    }

    /**
     * 检查是否为管理员
     */
    public static boolean isAdmin(HttpServletRequest request) {
        return "ADMIN".equals(getCurrentUserType(request));
    }
}
