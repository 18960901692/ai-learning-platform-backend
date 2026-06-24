package com.aicompanion.model.vo;

import lombok.Data;

/**
 * 学习统计 VO
 */
@Data
public class LearningStatsVO {

    /**
     * 累计学习时长（秒）
     */
    private Integer totalStudySeconds;

    /**
     * 正在学习的技能数
     */
    private Integer studyingSkillsCount;

    /**
     * 已完成的技能数
     */
    private Integer completedSkillsCount;

    /**
     * 进行中的学习计划数
     */
    private Integer activePlansCount;

    public LearningStatsVO() {
        this.totalStudySeconds = 0;
        this.studyingSkillsCount = 0;
        this.completedSkillsCount = 0;
        this.activePlansCount = 0;
    }
}
