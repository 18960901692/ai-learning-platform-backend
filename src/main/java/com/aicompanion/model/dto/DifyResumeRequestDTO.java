package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Dify 简历优化工作流请求
 */
@Data
public class DifyResumeRequestDTO {

    /**
     * 简历内容（传给 Dify Workflow 的 inputs.resume_content）
     */
    @NotBlank(message = "简历内容不能为空")
    @Size(max = 20000, message = "简历内容过长（最多 20000 字符），请精简后再试")
    private String resumeContent;
}
