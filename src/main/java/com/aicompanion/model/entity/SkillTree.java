package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 技能树实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill_tree")
public class SkillTree extends BaseEntity {

    /**
     * 技能名称
     */
    private String name;

    /**
     * 分类：FRONTEND/BACKEND/TOOL/BASIC
     */
    private String category;

    /**
     * 技能描述
     */
    private String description;

    /**
     * 状态：0-禁用 1-启用
     */
    private Integer status;
}
