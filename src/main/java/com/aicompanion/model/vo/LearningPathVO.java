package com.aicompanion.model.vo;

import lombok.Data;

/**
 * 学习路径推荐 VO
 */
@Data
public class LearningPathVO {

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 技能名称
     */
    private String skillName;

    /**
     * 技能类别
     */
    private String category;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 难度等级 1-5
     */
    private Integer level;

    /**
     * 推荐理由
     */
    private String reason;

    /**
     * 优先级（1-10，越高越优先）
     */
    private Integer priority;
}
