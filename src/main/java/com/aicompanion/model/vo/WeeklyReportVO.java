package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 成长周报 VO
 */
@Data
public class WeeklyReportVO {

    /** 报告类型：week / month */
    private String type;

    /** 报告范围 - 起始日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime startDate;

    /** 报告范围 - 结束日期 */
    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDateTime endDate;

    // ========== 学习时长统计 ==========

    /** 本期总学习时长（秒） */
    private Integer totalStudySeconds;

    /** 日均学习时长（秒） */
    private Integer avgDailySeconds;

    /** 上期总学习时长（秒），用于对比 */
    private Integer prevTotalStudySeconds;

    /** 环比变化百分比（%） */
    private Double changePercent;

    // ========== 技能进度 ==========

    /** 已点亮技能数 */
    private Integer masteredCount;

    /** 学习中技能数 */
    private Integer studyingCount;

    /** 未开始技能数 */
    private Integer notStartedCount;

    /** 本期新点亮技能数 */
    private Integer newMasteredCount;

    // ========== 学习活动 ==========

    /** AI 对话次数 */
    private Integer aiChatCount;

    /** AI 面试次数 */
    private Integer aiInterviewCount;

    /** 考试次数 */
    private Integer examCount;

    /** 连续打卡天数 */
    private Integer consecutiveDays;

    // ========== 本期学习记录列表（按时间倒序，最多 5 条） ==========

    private List<LearningRecordVO> recentRecords = new ArrayList<>();

    // ========== AI 智能分析 ==========

    /** AI 生成的成长分析 */
    private AiReportAnalysis aiAnalysis;

    public WeeklyReportVO() {
        this.totalStudySeconds = 0;
        this.avgDailySeconds = 0;
        this.prevTotalStudySeconds = 0;
        this.changePercent = 0.0;
        this.masteredCount = 0;
        this.studyingCount = 0;
        this.notStartedCount = 0;
        this.newMasteredCount = 0;
        this.aiChatCount = 0;
        this.aiInterviewCount = 0;
        this.examCount = 0;
        this.consecutiveDays = 0;
    }
}
