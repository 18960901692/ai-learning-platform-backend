package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 对话消息实体
 */
@Data
@TableName("chat_message")
public class ChatMessage {

    /**
     * 消息ID
     */
    @TableField("id")
    private Long id;

    /**
     * 会话ID（逻辑关联 chat_session.id）
     */
    @TableField("session_id")
    private Long sessionId;

    /**
     * 用户ID（逻辑关联 user.id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 角色: user/assistant/system
     */
    @TableField("role")
    private String role;

    /**
     * 消息内容
     */
    @TableField("content")
    private String content;

    /**
     * 使用的模型
     */
    @TableField("model")
    private String model;

    /**
     * 消耗Token数
     */
    @TableField("tokens_used")
    private Integer tokensUsed;

    /**
     * 逻辑删除: 0=正常, 1=已删除
     */
    @TableLogic
    private Integer deleted;

    /**
     * 创建时间
     */
    @TableField("create_time")
    private String createTime;
}
