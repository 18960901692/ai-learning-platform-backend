package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.entity.User;
import com.aicompanion.model.vo.LoginVO;
import com.aicompanion.model.vo.UserVO;
import com.aicompanion.service.AdminAuthService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * 管理员认证服务实现（统一 user 表，通过 role=ADMIN 区分）
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final UserMapper userMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 检查状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 4. 校验角色（管理后台只能 ADMIN 角色登录）
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "该账号为学生账号，请使用学生端登录");
        }

        // 5. 生成访问 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "ADMIN");

        // 6. 处理"记住密码"
        String refreshToken = null;
        if (Boolean.TRUE.equals(dto.getRememberMe())) {
            refreshToken = jwtUtil.generateRandomRefreshToken();
            LocalDateTime expireTime = LocalDateTime.now().plusDays(7);

            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpireTime(expireTime);
            userMapper.updateById(user);
        }

        // 7. 构建返回 VO
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setStatus(user.getStatus());
        userVO.setCreateTime(user.getCreateTime());

        return new LoginVO(token, refreshToken, userVO);
    }

    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        // 1. 解析刷新令牌
        Long userId = jwtUtil.getUserIdFromRefreshToken(dto.getRefreshToken());

        // 2. 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(401, "账号不存在");
        }

        // 3. 验证刷新令牌是否匹配
        if (!dto.getRefreshToken().equals(user.getRefreshToken())) {
            throw new BusinessException(401, "刷新令牌无效");
        }

        // 4. 检查刷新令牌是否过期
        if (user.getRefreshTokenExpireTime() == null ||
                user.getRefreshTokenExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "刷新令牌已过期");
        }

        // 5. 检查账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 6. 校验角色
        if (!"ADMIN".equals(user.getRole())) {
            throw new BusinessException(403, "该账号为学生账号，请使用学生端登录");
        }

        // 7. 生成新的访问 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), "ADMIN");

        // 8. 续期刷新令牌（滚动刷新）
        String newRefreshToken = jwtUtil.generateRandomRefreshToken();
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpireTime(LocalDateTime.now().plusDays(7));
        userMapper.updateById(user);

        // 9. 构建返回 VO
        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        userVO.setUsername(user.getUsername());
        userVO.setNickname(user.getNickname());
        userVO.setEmail(user.getEmail());
        userVO.setPhone(user.getPhone());
        userVO.setAvatar(user.getAvatar());
        userVO.setRole(user.getRole());
        userVO.setStatus(user.getStatus());
        userVO.setCreateTime(user.getCreateTime());

        return new LoginVO(token, newRefreshToken, userVO);
    }

    @Override
    public void logout(Long adminId) {
        User user = userMapper.selectById(adminId);
        if (user != null) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpireTime(null);
            userMapper.updateById(user);
            log.info("管理员登出成功: {}", user.getUsername());
        }
    }
}
