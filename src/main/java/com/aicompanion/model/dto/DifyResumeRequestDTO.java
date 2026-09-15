package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
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
    private String resumeContent;
}
