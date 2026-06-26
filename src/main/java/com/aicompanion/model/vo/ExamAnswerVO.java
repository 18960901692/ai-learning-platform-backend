package com.aicompanion.model.vo;

import lombok.Data;

@Data
public class ExamAnswerVO {

    private Long questionId;

    private String question;

    private Integer questionOrder;

    private String userAnswer;

    private Integer score;

    private String aiComment;
}
