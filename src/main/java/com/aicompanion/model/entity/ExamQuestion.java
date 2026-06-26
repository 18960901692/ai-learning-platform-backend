package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("exam_question")
public class ExamQuestion extends BaseEntity {

    private Long skillId;

    private String type;

    private String question;

    private String options;

    private String answer;

    private Integer score;

    private Integer difficulty;

    private String source;
}
