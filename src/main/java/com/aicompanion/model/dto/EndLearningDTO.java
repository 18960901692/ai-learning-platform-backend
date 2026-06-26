package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class EndLearningDTO {

    @NotNull(message = "学习记录ID不能为空")
    private Long recordId;

    /**
     * 前端本地计时的学习时长（秒）
     */
    private Integer studySeconds;
}
