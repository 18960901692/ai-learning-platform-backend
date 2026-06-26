package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class HeartbeatDTO {

    @NotNull(message = "学习记录ID不能为空")
    private Long recordId;
}
