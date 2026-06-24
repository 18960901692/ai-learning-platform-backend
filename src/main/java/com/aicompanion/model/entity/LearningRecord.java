package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

/**
 * 学习记录实体类
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("learning_record")
public class LearningRecord extends BaseEntity {

    /**
     * 用户ID
     */
    private Long userId;

    /**
     * 技能ID
     */
    private Long skillId;

    /**
     * 技能名称（冗余字段，减少JOIN）
     */
    private String skillName;

    /**
     * 来源类型：VIDEO/ARTICLE/PRACTICE/AI_CHAT
     */
    private String sourceType;

    /**
     * 学习进度百分比 0-100
     */
    private Integer progress;

    /**
     * 累计学习秒数
     */
    private Integer studySeconds;

    /**
     * 状态：0-未开始 1-学习中 2-已完成
     */
    private Integer status;

    /**
     * 首次学习时间
     */
    private LocalDateTime firstStudyTime;

    /**
     * 最近学习时间
     */
    private LocalDateTime lastStudyTime;

    /**
     * 完成时间
     */
    private LocalDateTime completeTime;
}
