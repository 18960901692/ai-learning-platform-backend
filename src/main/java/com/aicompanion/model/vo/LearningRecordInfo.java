package com.aicompanion.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 学习记录信息 VO（返回给 AI 的结构化数据）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LearningRecordInfo {

    /**
     * 总学习时长（秒）
     */
    private int totalStudySeconds;

    /**
     * 学习中的技能数
     */
    private int studyingCount;

    /**
     * 已完成的技能数
     */
    private int completedCount;

    /**
     * 连续打卡天数
     */
    private int consecutiveDays;

    /**
     * 各技能学习详情
     */
    private List<SkillLearningDetail> details;

    /**
     * 单个技能的学习详情
     */
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SkillLearningDetail {
        /**
         * 技能名称
         */
        private String skillName;

        /**
         * 学习进度（0-100）
         */
        private int progress;

        /**
         * 累计学习秒数
         */
        private int studySeconds;

        /**
         * 状态：学习中 / 已完成 / 未开始
         */
        private String status;
    }
}
