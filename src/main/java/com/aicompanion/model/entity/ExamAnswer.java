package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_answer")
public class ExamAnswer extends BaseEntity {

    private Long sessionId;

    private Long questionId;

    private Integer questionOrder;

    private String userAnswer;

    private Integer score;

    private String aiComment;
}
