package com.aicompanion.service.impl;

import com.aicompanion.mapper.AiCallLogMapper;
import com.aicompanion.model.entity.AiCallLog;
import com.aicompanion.service.AiCallLogService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;

/**
 * AI 调用日志服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCallLogServiceImpl implements AiCallLogService {

    private final AiCallLogMapper aiCallLogMapper;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * 异步保存调用日志（避免阻塞主业务流程）
     */
    @Async
    @Override
    public void saveLog(Long userId, String callType, Long durationMs, Integer success, String errorMessage) {
        try {
            AiCallLog log = new AiCallLog();
            log.setUserId(userId);
            log.setCallType(callType);
            log.setDurationMs(durationMs);
            log.setSuccess(success);
            log.setErrorMessage(errorMessage);
            log.setCreateTime(LocalDateTime.now().format(DATETIME_FORMATTER));

            aiCallLogMapper.insert(log);
        } catch (Exception e) {
            // 日志保存失败不应影响主业务
            log.error("保存 AI 调用日志失败", e);
        }
    }

    @Override
    public List<Map<String, Object>> countDailyByType(int days) {
        String startDate = LocalDateTime.now().minusDays(days).format(DATETIME_FORMATTER);
        return aiCallLogMapper.countDailyByType(startDate);
    }

    @Override
    public Map<String, Object> countTotal() {
        return aiCallLogMapper.countTotal();
    }

    @Override
    public List<Map<String, Object>> countByType() {
        return aiCallLogMapper.countByType();
    }

    @Override
    public List<Map<String, Object>> topUsers() {
        return aiCallLogMapper.topUsers();
    }
}
