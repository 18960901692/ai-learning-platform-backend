package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 知识点生成请求 DTO
 */
@Data
public class KnowledgePointRequestDTO {

    /**
     * 技能名称
     */
    @NotBlank(message = "技能名称不能为空")
    private String skillName;
}
