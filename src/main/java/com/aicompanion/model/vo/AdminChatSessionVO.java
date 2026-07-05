package com.aicompanion.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 管理后台 AI 会话记录 VO
 */
@Data
public class AdminChatSessionVO {

    @JsonSerialize(using = ToStringSerializer.class)
    private Long id;

    private String username;

    private String nickname;

    private String title;

    private String agentType;

    private int messageCount;

    private String createTime;

    private String updateTime;
}
