package com.aicompanion.common.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.concurrent.TimeUnit;

/**
 * Token 黑名单服务
 * 用于管理已登出用户的 Token，实现主动失效
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private static final String BLACKLIST_PREFIX = "token:blacklist:";

    private final StringRedisTemplate stringRedisTemplate;
    private final JwtUtil jwtUtil;

    /**
     * 将 Token 加入黑名单
     * @param token JWT Token
     * @param expirationSeconds 过期时间（秒），通常设置为 Token 剩余有效期
     */
    public void addToBlacklist(String token, long expirationSeconds) {
        if (token == null || token.isBlank()) {
            return;
        }

        String key = BLACKLIST_PREFIX + token;
        try {
            stringRedisTemplate.opsForValue().set(key, "1", expirationSeconds, TimeUnit.SECONDS);
            log.info("Token 已加入黑名单，过期时间: {} 秒", expirationSeconds);
        } catch (Exception e) {
            log.error("加入 Token 黑名单失败: {}", e.getMessage());
        }
    }

    /**
     * 检查 Token 是否在黑名单中
     * @param token JWT Token
     * @return true=在黑名单中（已失效），false=不在黑名单中（有效）
     */
    public boolean isBlacklisted(String token) {
        if (token == null || token.isBlank()) {
            return false;
        }

        try {
            String key = BLACKLIST_PREFIX + token;
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(key));
        } catch (Exception e) {
            log.error("检查 Token 黑名单失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 计算 Token 剩余有效期（秒）
     * @param token JWT Token
     * @return 剩余秒数，如果已过期则返回 0
     */
    public long getTokenRemainingSeconds(String token) {
        if (token == null || token.isBlank()) {
            return 0;
        }

        try {
            long expiration = jwtUtil.getExpiration(token);
            long currentTime = System.currentTimeMillis();
            long remaining = (expiration - currentTime) / 1000;
            return Math.max(remaining, 0);
        } catch (Exception e) {
            log.error("计算 Token 剩余时间失败: {}", e.getMessage());
            return 0;
        }
    }
}
