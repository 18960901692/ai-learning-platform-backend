package com.aicompanion.service.impl;

import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.service.CheckInService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.Year;

@Slf4j
@Service
@RequiredArgsConstructor
public class CheckInServiceImpl implements CheckInService {

    private final StringRedisTemplate redisTemplate;
    private final LearningRecordMapper learningRecordMapper;

    private static final String KEY_PREFIX = "checkin:";
    private static final int MAX_DAYS = 365;

    @Override
    public void checkIn(Long userId) {
        try {
            LocalDate today = LocalDate.now();
            String key = buildKey(userId, today.getYear());
            redisTemplate.opsForValue().setBit(key, today.getDayOfYear() - 1, true);
            log.info("Redis 打卡成功: userId={}, key={}, dayOfYear={}", userId, key, today.getDayOfYear());
        } catch (Exception e) {
            log.warn("Redis 打卡失败，降级使用 SQL: userId={}, error={}", userId, e.getMessage());
        }
    }

    @Override
    public int getConsecutiveDays(Long userId) {
        try {
            return getConsecutiveDaysFromRedis(userId);
        } catch (Exception e) {
            log.warn("Redis 查询连续天数失败，降级 SQL: userId={}, error={}", userId, e.getMessage());
            return getConsecutiveDaysFromDb(userId);
        }
    }

    private int getConsecutiveDaysFromRedis(Long userId) {
        int count = 0;
        LocalDate today = LocalDate.now();

        for (int i = 0; i < MAX_DAYS; i++) {
            LocalDate date = today.minusDays(i);
            String key = buildKey(userId, date.getYear());
            Boolean checked = redisTemplate.opsForValue().getBit(key, date.getDayOfYear() - 1);

            if (Boolean.TRUE.equals(checked)) {
                count++;
            } else {
                break;
            }
        }

        log.info("Redis 查询连续天数: userId={}, count={}", userId, count);
        return count;
    }

    private int getConsecutiveDaysFromDb(Long userId) {
        Integer result = learningRecordMapper.selectConsecutiveDays(userId);
        return result != null ? result : 0;
    }

    private String buildKey(Long userId, int year) {
        return KEY_PREFIX + userId + ":" + year;
    }
}