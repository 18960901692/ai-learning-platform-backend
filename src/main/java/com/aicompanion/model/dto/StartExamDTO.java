package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class StartExamDTO {

    @NotNull(message = "技能ID不能为空")
    private Long skillId;
}
