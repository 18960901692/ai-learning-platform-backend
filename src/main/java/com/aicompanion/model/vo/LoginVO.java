package com.aicompanion.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录响应 VO（token + 用户信息 + 刷新令牌）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class LoginVO {

    /**
     * 访问令牌（短期，24小时）
     */
    private String token;

    /**
     * 刷新令牌（长期，7天），仅 rememberMe=true 时返回
     */
    private String refreshToken;

    /**
     * 用户信息
     */
    private UserVO user;
}
