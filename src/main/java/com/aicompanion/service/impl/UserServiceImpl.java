package com.aicompanion.service.impl;

import com.aicompanion.dto.LoginDTO;
import com.aicompanion.dto.RegisterDTO;
import com.aicompanion.dto.UserDTO;
import com.aicompanion.entity.User;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.service.UserService;
import com.aicompanion.util.JwtUtil;
import com.aicompanion.vo.LoginVO;
import com.aicompanion.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * 用户服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserMapper userMapper;
    private final BCryptPasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    /**
     * 用户注册
     */
    @Override
    public void register(RegisterDTO dto) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new RuntimeException("用户名已存在");
        }

        // 2. 构建用户并加密密码
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getUsername());  // 默认昵称同用户名
        user.setRole("STUDENT");               // 默认角色为学生
        user.setStatus(1);                     // 默认正常状态

        // 3. 插入数据库
        userMapper.insert(user);
        log.info("用户注册成功: {}", dto.getUsername());
    }

    /**
     * 用户登录
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        // 1. 根据用户名查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 2. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new RuntimeException("账号已被禁用，请联系管理员");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new RuntimeException("用户名或密码错误");
        }

        // 4. 生成 JWT Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 构建登录响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setUser(toUserVO(user));

        log.info("用户登录成功: {}", dto.getUsername());
        return loginVO;
    }

    /**
     * 获取用户信息
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }
        return toUserVO(user);
    }

    /**
     * 更新用户信息
     */
    @Override
    public UserVO updateUser(Long userId, UserDTO dto) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new RuntimeException("用户不存在");
        }

        // 仅更新非空字段
        if (dto.getNickname() != null) {
            user.setNickname(dto.getNickname());
        }
        if (dto.getEmail() != null) {
            user.setEmail(dto.getEmail());
        }
        if (dto.getPhone() != null) {
            user.setPhone(dto.getPhone());
        }
        if (dto.getAvatar() != null) {
            user.setAvatar(dto.getAvatar());
        }

        userMapper.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);
        return toUserVO(user);
    }

    /**
     * Entity → UserVO（过滤密码）
     */
    private UserVO toUserVO(User user) {
        UserVO vo = new UserVO();
        vo.setId(user.getId());
        vo.setUsername(user.getUsername());
        vo.setNickname(user.getNickname());
        vo.setEmail(user.getEmail());
        vo.setPhone(user.getPhone());
        vo.setAvatar(user.getAvatar());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }
}
