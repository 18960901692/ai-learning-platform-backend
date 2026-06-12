package com.aicompanion.service;

import com.aicompanion.dto.LoginDTO;
import com.aicompanion.dto.RegisterDTO;
import com.aicompanion.dto.UserDTO;
import com.aicompanion.vo.LoginVO;
import com.aicompanion.vo.UserVO;

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
     * 获取用户信息
     */
    UserVO getUserInfo(Long userId);

    /**
     * 更新用户信息
     */
    UserVO updateUser(Long userId, UserDTO dto);
}
