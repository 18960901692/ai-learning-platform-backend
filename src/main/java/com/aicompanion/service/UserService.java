package com.aicompanion.service;

import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.dto.RegisterDTO;
import com.aicompanion.model.dto.UserDTO;
import com.aicompanion.model.vo.LoginVO;
import com.aicompanion.model.vo.UserVO;

/**
 * 用户服务接口
 */
public interface UserService {

    /**
     * 用户注册
     */
    void register(RegisterDTO dto);

    /**
     * 用户登录
     */
    LoginVO login(LoginDTO dto);

    /**
     * 刷新令牌（记住密码免登录）
     */
    LoginVO refreshToken(RefreshTokenDTO dto);

    /**
     * 获取用户信息
     */
    UserVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    UserVO updateUser(Long userId, UserDTO dto);

    /**
     * 退出登录（清除刷新令牌）
     */
    void logout(Long userId);
}
