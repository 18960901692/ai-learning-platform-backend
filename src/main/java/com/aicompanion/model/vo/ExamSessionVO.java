package com.aicompanion.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 考核结果 VO
 */
@Data
public class ExamSessionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;

    @JsonSerialize(using = ToStringSerializer.class)
    private Long skillId;

    private String skillName;

    private String status;

    private Integer totalScore;

    private Integer passScore;

    private Boolean passed;

    private Integer totalQuestions;

    private Integer answeredCount;

    private String aiFeedback;

    private LocalDateTime startTime;

    private LocalDateTime endTime;
}
