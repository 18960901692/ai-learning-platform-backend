package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Dify 考核相关请求（对话、出题、阅卷通用）
 */
@Data
public class DifyChatRequestDTO {

    /**
     * 技能名称（传给 Dify 的 inputs.skill_name）
     */
    @NotBlank(message = "技能名称不能为空")
    private String skillName;

    /**
     * 用户消息 / 阅卷时的 query / 出题时可留空
     */
    private String query;

    /**
     * Dify 会话 ID（多轮对话时传入）
     */
    private String conversationId;

    /**
     * 阅卷时的完整对话记录（inputs.conversation），其他场景传 null
     */
    private String conversation;
}
