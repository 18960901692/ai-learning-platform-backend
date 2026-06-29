package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 对话会话实体
 */
@Data
@TableName("chat_session")
public class ChatSession {

    /**
     * 会话ID
     */
    @TableField("id")
    private Long id;

    /**
     * 用户ID（逻辑关联 user.id）
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 会话标题
     */
    @TableField("title")
    private String title;

    /**
     * Agent类型: CHAT/ASSESSMENT/PLANNING/INTERVIEW
     */
    @TableField("agent_type")
    private String agentType;

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

    /**
     * 更新时间
     */
    @TableField("update_time")
    private String updateTime;
}
