package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 用户技能关联实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("user_skill")
public class UserSkill extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 掌握程度 0-5
     */
    private Integer level;

    /**
     * 状态: 0=未开始, 1=学习中, 2=已掌握
     */
    private Integer status;
}
