package com.aicompanion.service;

import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.vo.LoginVO;

/**
 * 管理员认证服务
 */
public interface AdminAuthService {

    /**
     * 管理员登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新令牌（记住密码免登录）
     */
    LoginVO refreshToken(RefreshTokenDTO dto);

    /**
     * 管理员登出
     */
    void logout(Long adminId);
}
