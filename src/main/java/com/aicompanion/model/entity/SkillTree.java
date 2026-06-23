package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 技能树实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("skill")
public class SkillTree extends BaseEntity {

    /**
     * 技能名称
     */
    private String name;

    /**
     * 技能类别：前端开发/后端开发/数据库/运维部署
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
     * 父技能ID（0表示顶级）
     */
    private Long parentId;

    /**
     * skill 表没有 update_time 字段，排除父类的 updateTime
     */
    @TableField(exist = false)
    private LocalDateTime updateTime;
}
