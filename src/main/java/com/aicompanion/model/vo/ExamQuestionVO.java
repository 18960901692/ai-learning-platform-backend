package com.aicompanion.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 考核题目 VO（返回给前端，不含答案）
 */
@Data
public class ExamQuestionVO {

    private Long id;

    private String type;

    private String question;

    private List<String> options;

    private Integer score;

    private Integer difficulty;

    private Integer questionOrder;
}
