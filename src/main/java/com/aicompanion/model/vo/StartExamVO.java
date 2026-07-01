package com.aicompanion.model.vo;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.Data;

/**
 * 开始考核响应 VO
 * 注：题目由前端从 Dify 获取，后端只返回会话 ID
 */
@Data
public class StartExamVO {

    /**
     * 会话ID（序列化为 String，避免前端 JS 精度丢失）
     */
    @JsonSerialize(using = ToStringSerializer.class)
    private Long sessionId;
}