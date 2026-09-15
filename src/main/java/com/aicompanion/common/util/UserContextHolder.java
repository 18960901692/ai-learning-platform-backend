package com.aicompanion.common.util;

/**
 * 统一管理"当前请求的用户ID"——基于 ThreadLocal 替代 Spring AI Tool 里原来的 volatile 字段。
 *
 * <p>为什么不能用 volatile：volatile 只保证可见性，不保证原子性。
 * 两个并发请求 A / B 同时进来，A 刚写完 userId=1，B 立刻写成 2，
 * A 的 Tool 就会查到用户 B 的数据（数据越权）。</p>
 *
 * <p>为什么不能用 SecurityUtil.getCurrentUserId()：它依赖 RequestContextHolder（Servlet request 属性），
 * 在 CompletableFuture.runAsync() 等异步线程里 request 上下文会丢失，拿不到。</p>
 *
 * <p>ThreadLocal 方案：
 * <ul>
 *   <li>同步方法：set() → try { ... } finally { clear() }</li>
 *   <li>异步方法（runAsync）：主线程 set → 异步任务内部手动 set 一次复制过去 → finally clear</li>
 * </ul></p>
 */
public class UserContextHolder {

    private static final ThreadLocal<Long> USER_ID = new ThreadLocal<>();

    /**
     * 存入当前请求的 userId（由拦截器或 Service 在调用链入口处设置）
     */
    public static void set(Long userId) {
        USER_ID.set(userId);
    }

    /**
     * 获取当前线程的 userId
     *
     * @return userId；未设置或已被清理时返回 null
     */
    public static Long get() {
        return USER_ID.get();
    }

    /**
     * 清理 ThreadLocal —— 必须在 finally 里调，防止线程池复用导致旧值残留（ThreadLocal 泄漏）
     */
    public static void clear() {
        USER_ID.remove();
    }
}
