package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class SubmitAnswerDTO {

    @NotNull(message = "会话ID不能为空")
    private Long sessionId;

    private List<SingleAnswerDTO> answers;

    @Data
    public static class SingleAnswerDTO {
        @NotNull
        private Long questionId;

        private String userAnswer;
    }
}
