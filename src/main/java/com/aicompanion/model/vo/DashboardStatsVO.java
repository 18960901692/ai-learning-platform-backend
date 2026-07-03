package com.aicompanion.model.vo;

import lombok.Data;

import java.util.List;
import java.util.Map;

/**
 * 管理后台首页统计数据 VO
 */
@Data
public class DashboardStatsVO {

    /**
     * 用户总数
     */
    private Long totalUsers;

    /**
     * 技能总点亮数
     */
    private Long totalLitSkills;

    /**
     * 今日活跃用户数
     */
    private Long todayActiveUsers;

    /**
     * 今日 AI 调用次数
     */
    private Long todayAiCalls;

    /**
     * 近 N 天用户增长趋势
     */
    private List<Map<String, Object>> userGrowth;

    /**
     * 最近活跃学习用户 Top 5
     */
    private List<Map<String, Object>> recentLearners;

    /**
     * 最近考核情况 Top 5
     */
    private List<Map<String, Object>> recentExams;
}