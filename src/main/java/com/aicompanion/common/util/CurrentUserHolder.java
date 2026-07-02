package com.aicompanion.common.util;

/**
 * 当前用户ID的线程上下文（解决异步线程中无法获取 RequestContextHolder 的问题）
 */
public class CurrentUserHolder {

    private static final ThreadLocal<Long> CURRENT_USER_ID = new ThreadLocal<>();

    /**
     * 设置当前线程的用户ID
     */
    public static void setUserId(Long userId) {
        CURRENT_USER_ID.set(userId);
    }

    /**
     * 获取当前线程的用户ID（可能为 null）
     */
    public static Long getUserId() {
        return CURRENT_USER_ID.get();
    }

    /**
     * 清除当前线程的用户ID（防止内存泄漏）
     */
    public static void clear() {
        CURRENT_USER_ID.remove();
    }
}
