package com.aicompanion.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 学习记录 VO
 */
@Data
public class LearningRecordVO {

    private Long id;
    private Long userId;
    private Long skillId;
    private String skillName;
    private String sourceType;
    private Integer progress;
    private Integer studySeconds;
    private Integer status;
    private LocalDateTime firstStudyTime;
    private LocalDateTime lastStudyTime;
    private LocalDateTime completeTime;
    private LocalDateTime createTime;
}
