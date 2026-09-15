package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dify /chat-messages API 的 blocking 模式响应（只映射关心的字段）
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyChatResponseVO {

    /**
     * AI 回复文本
     */
    private String answer;

    /**
     * 会话 ID（多轮对话时需传回）
     */
    @JsonProperty("conversation_id")
    private String conversationId;

    /**
     * 消息唯一 ID
     */
    @JsonProperty("message_id")
    private String messageId;
}
