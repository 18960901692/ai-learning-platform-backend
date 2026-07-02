package com.aicompanion.model.entity;

import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * AI 调用日志实体
 */
@Data
@TableName("ai_call_log")
public class AiCallLog {

    private Long id;

    /**
     * 用户ID
     */
    @TableField("user_id")
    private Long userId;

    /**
     * 调用类型: CHAT / CHAT_STREAM / INTERVIEW / KNOWLEDGE_POINT / LEARNING_PATH
     */
    @TableField("call_type")
    private String callType;

    /**
     * 耗时（毫秒）
     */
    @TableField("duration_ms")
    private Long durationMs;

    /**
     * 是否成功: 1=成功, 0=失败
     */
    @TableField("success")
    private Integer success;

    /**
     * 错误信息
     */
    @TableField("error_message")
    private String errorMessage;

    /**
     * 调用时间
     */
    @TableField("create_time")
    private String createTime;
}
