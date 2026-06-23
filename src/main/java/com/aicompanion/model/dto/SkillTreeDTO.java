package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 技能树创建/更新 DTO
 */
@Data
public class SkillTreeDTO {

    @NotBlank(message = "技能名称不能为空")
    private String name;

    @NotBlank(message = "分类不能为空")
    private String category;

    private String description;

    private Integer level;

    private Long parentId;
}
