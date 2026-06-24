package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.mapper.AdminMapper;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.entity.Admin;
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
 * 管理员认证服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminAuthServiceImpl implements AdminAuthService {

    private final AdminMapper adminMapper;
    private final JwtUtil jwtUtil;
    private final PasswordEncoder passwordEncoder;

    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 查询管理员
        LambdaQueryWrapper<Admin> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Admin::getUsername, dto.getUsername());
        Admin admin = adminMapper.selectOne(wrapper);

        if (admin == null) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 验证密码
        if (!passwordEncoder.matches(dto.getPassword(), admin.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 3. 检查状态
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 4. 生成访问 Token
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");

        // 5. 处理"记住密码"
        String refreshToken = null;
        if (Boolean.TRUE.equals(dto.getRememberMe())) {
            refreshToken = jwtUtil.generateRandomRefreshToken();
            LocalDateTime expireTime = LocalDateTime.now().plusDays(7);

            admin.setRefreshToken(refreshToken);
            admin.setRefreshTokenExpireTime(expireTime);
            adminMapper.updateById(admin);
        }

        // 6. 构建返回 VO
        UserVO userVO = new UserVO();
        userVO.setId(admin.getId());
        userVO.setUsername(admin.getUsername());
        userVO.setNickname(admin.getNickname());
        userVO.setEmail(admin.getEmail());
        userVO.setPhone(admin.getPhone());
        userVO.setAvatar(admin.getAvatar());
        userVO.setStatus(admin.getStatus());
        userVO.setCreateTime(admin.getCreateTime());

        return new LoginVO(token, refreshToken, userVO);
    }

    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        // 1. 解析刷新令牌
        Long adminId = jwtUtil.getUserIdFromRefreshToken(dto.getRefreshToken());

        // 2. 查询管理员
        Admin admin = adminMapper.selectById(adminId);
        if (admin == null) {
            throw new BusinessException(401, "账号不存在");
        }

        // 3. 验证刷新令牌是否匹配
        if (!dto.getRefreshToken().equals(admin.getRefreshToken())) {
            throw new BusinessException(401, "刷新令牌无效");
        }

        // 4. 检查刷新令牌是否过期
        if (admin.getRefreshTokenExpireTime() == null ||
                admin.getRefreshTokenExpireTime().isBefore(LocalDateTime.now())) {
            throw new BusinessException(401, "刷新令牌已过期");
        }

        // 5. 检查账号状态
        if (admin.getStatus() == null || admin.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用");
        }

        // 6. 生成新的访问 Token
        String token = jwtUtil.generateToken(admin.getId(), admin.getUsername(), "ADMIN");

        // 7. 续期刷新令牌（滚动刷新）
        String newRefreshToken = jwtUtil.generateRandomRefreshToken();
        admin.setRefreshToken(newRefreshToken);
        admin.setRefreshTokenExpireTime(LocalDateTime.now().plusDays(7));
        adminMapper.updateById(admin);

        // 8. 构建返回 VO
        UserVO userVO = new UserVO();
        userVO.setId(admin.getId());
        userVO.setUsername(admin.getUsername());
        userVO.setNickname(admin.getNickname());
        userVO.setEmail(admin.getEmail());
        userVO.setPhone(admin.getPhone());
        userVO.setAvatar(admin.getAvatar());
        userVO.setStatus(admin.getStatus());
        userVO.setCreateTime(admin.getCreateTime());

        return new LoginVO(token, newRefreshToken, userVO);
    }

    @Override
    public void logout(Long adminId) {
        Admin admin = adminMapper.selectById(adminId);
        if (admin != null) {
            admin.setRefreshToken(null);
            admin.setRefreshTokenExpireTime(null);
            adminMapper.updateById(admin);
            log.info("管理员登出成功: {}", admin.getUsername());
        }
    }
}
