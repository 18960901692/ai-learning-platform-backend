package com.aicompanion.service;

import com.aicompanion.model.entity.AiCallLog;

import java.util.List;
import java.util.Map;

/**
 * AI 调用日志服务接口
 */
public interface AiCallLogService {

    /**
     * 保存调用日志
     */
    void saveLog(Long userId, String callType, Long durationMs, Integer success, String errorMessage);

    /**
     * 按天统计近 N 天的调用次数（按类型分组）
     */
    List<Map<String, Object>> countDailyByType(int days);

    /**
     * 统计总调用次数和成功次数
     */
    Map<String, Object> countTotal();

    /**
     * 按类型统计调用次数
     */
    List<Map<String, Object>> countByType();

    /**
     * 调用次数最多的用户 TOP10
     */
    List<Map<String, Object>> topUsers();
}
