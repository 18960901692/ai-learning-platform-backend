package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_session")
public class ExamSession extends BaseEntity {

    private Long userId;

    private Long skillId;

    private String status;

    private Integer totalScore;

    private Integer passScore;

    private Integer totalQuestions;

    private Integer answeredCount;

    private String aiFeedback;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
