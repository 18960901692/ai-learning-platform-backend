package com.aicompanion.service;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.model.dto.CreateUserDTO;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.dto.RegisterDTO;
import com.aicompanion.model.dto.UpdateUserDTO;
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

    /**
     * 更新用户头像
     */
    UserVO updateAvatar(Long userId, String avatarUrl);

    /**
     * 分页查询用户列表
     */
    PageResult<UserVO> getUserList(int page, int pageSize, String keyword, String role);

    /**
     * 管理员新增用户
     */
    UserVO createUser(CreateUserDTO dto);

    /**
     * 管理员修改用户信息
     */
    UserVO updateUserById(Long userId, UpdateUserDTO dto);

    /**
     * 管理员删除用户
     */
    void deleteUser(Long userId);
}
