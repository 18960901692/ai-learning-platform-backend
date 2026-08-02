package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 考核对话请求 DTO（包含技能名称）
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class ExamChatRequestDTO extends AiChatRequestDTO {

    /**
     * 技能名称（用于 AI 出题）
     */
    @NotBlank(message = "技能名称不能为空")
    private String skillName;
}