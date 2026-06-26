package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Dify 阅卷结果 DTO（鸿蒙端调 Dify 后传给后端保存）
 */
@Data
public class DifyGradeDTO {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    /** Dify 返回的评分报告文本 */
    @NotNull(message = "评分报告不能为空")
    private String text;

    /** Dify 返回的得分（0-100） */
    @NotNull(message = "得分不能为空")
    private Integer score;

    /** 用户答案列表 */
    private List<SingleAnswerDTO> answers;

    @Data
    public static class SingleAnswerDTO {
        @NotNull
        private Long questionId;
        private String userAnswer;
    }
}
