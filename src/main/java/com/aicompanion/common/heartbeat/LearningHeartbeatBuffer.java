package com.aicompanion.common.heartbeat;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

/**
 * 学习心跳 Redis 缓冲层
 *
 * <p>心跳写入只累加 Redis 计数器（INCRBY 原子操作），避免每次心跳都打 DB。
 * 由定时任务批量刷盘到 MySQL，或在结束学习时强制 flush，保障数据不丢失。</p>
 *
 * <p>Key 设计：{@code learning:heartbeat:buffer:{recordId}} → 待刷盘秒数</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningHeartbeatBuffer {

    private static final String BUFFER_KEY_PREFIX = "learning:heartbeat:buffer:";
    private static final String BUFFER_KEY_PATTERN = BUFFER_KEY_PREFIX + "*";

    private final StringRedisTemplate stringRedisTemplate;

    /**
     * 构造指定学习记录的缓冲 Key
     */
    private String buildKey(Long recordId) {
        return BUFFER_KEY_PREFIX + recordId;
    }

    /**
     * 从 Key 中解析出 recordId
     */
    private Long parseRecordId(String key) {
        try {
            return Long.parseLong(key.substring(BUFFER_KEY_PREFIX.length()));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * 累加心跳秒数到 Redis 缓冲（原子操作，线程安全）
     *
     * @param recordId 学习记录ID
     * @param seconds  本次心跳累加秒数
     */
    public void accumulate(Long recordId, int seconds) {
        String key = buildKey(recordId);
        Long newVal = stringRedisTemplate.opsForValue().increment(key, seconds);
        log.debug("心跳缓冲累加: recordId={}, +{}s, buffered={}", recordId, seconds, newVal);
    }

    /**
     * 查询当前缓冲区中尚未刷盘的秒数
     */
    public int getBufferedSeconds(Long recordId) {
        String val = stringRedisTemplate.opsForValue().get(buildKey(recordId));
        if (val == null || val.isEmpty()) {
            return 0;
        }
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("缓冲秒数格式异常: recordId={}, val={}", recordId, val);
            return 0;
        }
    }

    /**
     * 取出并清空指定学习记录的缓冲秒数（用于结束学习时强制刷盘）
     *
     * @return 待刷盘秒数（无缓冲返回 0）
     */
    public int drain(Long recordId) {
        String key = buildKey(recordId);
        String val = stringRedisTemplate.opsForValue().get(key);
        if (val == null || val.isEmpty()) {
            return 0;
        }
        stringRedisTemplate.delete(key);
        try {
            return Integer.parseInt(val);
        } catch (NumberFormatException e) {
            log.warn("缓冲秒数格式异常: recordId={}, val={}", recordId, val);
            return 0;
        }
    }

    /**
     * 扫描所有缓冲 Key，取出全部待刷盘秒数并清空（用于定时批量刷盘）
     *
     * <p>使用 SCAN 遍历，避免 KEYS 阻塞 Redis。</p>
     *
     * @return recordId → 待刷盘秒数
     */
    public Map<Long, Integer> drainAll() {
        Map<Long, Integer> result = new HashMap<>();
        ScanOptions options = ScanOptions.scanOptions().match(BUFFER_KEY_PATTERN).count(100).build();
        try (Cursor<String> cursor = stringRedisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                String key = cursor.next();
                Long recordId = parseRecordId(key);
                if (recordId == null) {
                    continue;
                }
                String val = stringRedisTemplate.opsForValue().get(key);
                if (val == null || val.isEmpty()) {
                    continue;
                }
                try {
                    int seconds = Integer.parseInt(val);
                    if (seconds > 0) {
                        result.put(recordId, seconds);
                    }
                } catch (NumberFormatException e) {
                    log.warn("缓冲秒数格式异常: key={}, val={}", key, val);
                }
                stringRedisTemplate.delete(key);
            }
        }
        return result;
    }
}
