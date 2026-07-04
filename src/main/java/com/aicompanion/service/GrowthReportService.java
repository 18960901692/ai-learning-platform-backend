package com.aicompanion.service;

import com.aicompanion.model.vo.AiReportAnalysis;
import com.aicompanion.model.vo.WeeklyReportVO;

/**
 * 成长周报服务接口
 */
public interface GrowthReportService {

    /**
     * 生成成长周报统计数据（不含 AI 分析，快速返回）
     *
     * @param userId 用户ID
     * @param type   报告类型：week / month
     */
    WeeklyReportVO generateReportStats(Long userId, String type);

    /**
     * 生成 AI 成长分析（独立调用 Spring AI，较慢）
     *
     * @param userId 用户ID
     * @param type   报告类型：week / month
     */
    AiReportAnalysis generateAiAnalysis(Long userId, String type);
}
