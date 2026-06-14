package com.aicompanion.common.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 安全工具类
 */
public class SecurityUtil {

    private static final String USER_ID_ATTR = "userId";
    private static final String USER_ROLE_ATTR = "userRole";
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
     * 从 Request 中获取当前用户角色
     */
    public static String getCurrentUserRole(HttpServletRequest request) {
        Object role = request.getAttribute(USER_ROLE_ATTR);
        return role != null ? (String) role : "STUDENT";
    }

    /**
     * 将用户信息存入 Request
     */
    public static void setCurrentUser(HttpServletRequest request, Long userId, String role) {
        request.setAttribute(USER_ID_ATTR, userId);
        request.setAttribute(USER_ROLE_ATTR, role);
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
        return "ADMIN".equals(getCurrentUserRole(request));
    }

    /**
     * 检查是否为教师
     */
    public static boolean isTeacher(HttpServletRequest request) {
        return "TEACHER".equals(getCurrentUserRole(request));
    }
}
