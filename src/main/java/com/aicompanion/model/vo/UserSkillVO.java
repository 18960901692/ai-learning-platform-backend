package com.aicompanion.model.vo;

import lombok.Data;

/**
 * 用户技能 VO
 */
@Data
public class UserSkillVO {

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
     * 难度等级 1-5
     */
    private Integer difficultyLevel;

    /**
     * 掌握程度 0-5
     */
    private Integer level;

    /**
     * 状态: 0=未开始, 1=学习中, 2=已掌握
     */
    private Integer status;
}
