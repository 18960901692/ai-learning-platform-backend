package com.aicompanion.model.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * AI 对话请求 DTO
 */
@Data
public class AiChatRequestDTO {

    /**
     * 会话 ID（用于多轮对话上下文）
     */
    @NotBlank(message = "会话 ID 不能为空")
    private String sessionId;

    /**
     * 用户消息
     */
    @NotBlank(message = "消息内容不能为空")
    private String message;
}
