package com.aicompanion.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 考核结果 VO
 */
@Data
public class ExamSessionVO {

    private Long sessionId;

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

    private List<ExamAnswerVO> answers;
}
