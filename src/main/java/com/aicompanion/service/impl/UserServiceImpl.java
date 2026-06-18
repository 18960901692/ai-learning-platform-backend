package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.model.dto.CreateUserDTO;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.dto.RegisterDTO;
import com.aicompanion.model.dto.UserDTO;
import com.aicompanion.model.entity.User;
import com.aicompanion.model.vo.LoginVO;
import com.aicompanion.model.vo.UserVO;
import com.aicompanion.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

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
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. 检查邮箱是否已存在
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, dto.getEmail());
        Long emailCount = userMapper.selectCount(emailWrapper);
        if (emailCount > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 3. 构建用户并加密密码
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setEmail(dto.getEmail());
        user.setNickname(dto.getUsername());
        user.setRole("STUDENT");
        user.setStatus(1);

        // 4. 插入数据库
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
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 2. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 3. 校验密码
        if (!passwordEncoder.matches(dto.getPassword(), user.getPassword())) {
            throw new BusinessException(401, "用户名或密码错误");
        }

        // 4. 生成访问 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 处理"记住密码"
        String refreshToken = null;
        if (Boolean.TRUE.equals(dto.getRememberMe())) {
            refreshToken = jwtUtil.generateRandomRefreshToken();
            user.setRefreshToken(refreshToken);
            user.setRefreshTokenExpireTime(LocalDateTime.now().plusDays(7));
            userMapper.updateById(user);
            log.info("用户开启记住密码: {}", dto.getUsername());
        }

        // 6. 构建登录响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(refreshToken);
        loginVO.setUser(toUserVO(user));

        log.info("用户登录成功: {}", dto.getUsername());
        return loginVO;
    }

    /**
     * 刷新令牌（记住密码免登录）
     */
    @Override
    public LoginVO refreshToken(RefreshTokenDTO dto) {
        // 1. 根据刷新令牌查询用户
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getRefreshToken, dto.getRefreshToken());
        User user = userMapper.selectOne(wrapper);

        if (user == null) {
            throw new BusinessException(401, "刷新令牌无效，请重新登录");
        }

        // 2. 检查刷新令牌是否过期
        if (user.getRefreshTokenExpireTime() == null
                || user.getRefreshTokenExpireTime().isBefore(LocalDateTime.now())) {
            // 清除过期的刷新令牌
            user.setRefreshToken(null);
            user.setRefreshTokenExpireTime(null);
            userMapper.updateById(user);
            throw new BusinessException(401, "刷新令牌已过期，请重新登录");
        }

        // 3. 校验账号状态
        if (user.getStatus() == null || user.getStatus() != 1) {
            throw new BusinessException(403, "账号已被禁用，请联系管理员");
        }

        // 4. 生成新的访问 Token
        String token = jwtUtil.generateToken(user.getId(), user.getUsername(), user.getRole());

        // 5. 续期刷新令牌（滚动刷新）
        String newRefreshToken = jwtUtil.generateRandomRefreshToken();
        user.setRefreshToken(newRefreshToken);
        user.setRefreshTokenExpireTime(LocalDateTime.now().plusDays(7));
        userMapper.updateById(user);

        // 6. 构建登录响应
        LoginVO loginVO = new LoginVO();
        loginVO.setToken(token);
        loginVO.setRefreshToken(newRefreshToken);
        loginVO.setUser(toUserVO(user));

        log.info("令牌刷新成功: {}", user.getUsername());
        return loginVO;
    }

    /**
     * 获取用户信息
     */
    @Override
    public UserVO getUserInfo(Long userId) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
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
            throw new BusinessException(404, "用户不存在");
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
        if (dto.getProfession() != null) {
            user.setProfession(dto.getProfession());
        }
        if (dto.getBio() != null) {
            user.setBio(dto.getBio());
        }

        userMapper.updateById(user);
        log.info("用户信息更新成功: userId={}", userId);
        return toUserVO(user);
    }

    /**
     * 退出登录（清除刷新令牌）
     */
    @Override
    public void logout(Long userId) {
        User user = userMapper.selectById(userId);
        if (user != null && user.getRefreshToken() != null) {
            user.setRefreshToken(null);
            user.setRefreshTokenExpireTime(null);
            userMapper.updateById(user);
            log.info("用户退出登录: userId={}", userId);
        }
    }

    /**
     * 更新用户头像
     */
    @Override
    public UserVO updateAvatar(Long userId, String avatarUrl) {
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new BusinessException(404, "用户不存在");
        }
        user.setAvatar(avatarUrl);
        userMapper.updateById(user);
        log.info("用户头像更新成功: userId={}, avatarUrl={}", userId, avatarUrl);
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
        vo.setProfession(user.getProfession());
        vo.setBio(user.getBio());
        vo.setRole(user.getRole());
        vo.setStatus(user.getStatus());
        vo.setCreateTime(user.getCreateTime());
        return vo;
    }

    /**
     * 分页查询用户列表
     */
    @Override
    public PageResult<UserVO> getUserList(int page, int pageSize, String keyword, String role) {
        Page<User> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(User::getUsername, keyword)
                    .or()
                    .like(User::getNickname, keyword)
            );
        }

        if (StringUtils.hasText(role)) {
            wrapper.eq(User::getRole, role);
        }

        wrapper.orderByDesc(User::getCreateTime);

        Page<User> result = userMapper.selectPage(pageParam, wrapper);

        List<UserVO> voList = result.getRecords().stream()
                .map(this::toUserVO)
                .toList();

        return PageResult.of(result.getTotal(), voList, result.getCurrent(), result.getSize());
    }

    /**
     * 管理员新增用户
     */
    @Override
    public UserVO createUser(CreateUserDTO dto) {
        // 1. 检查用户名是否已存在
        LambdaQueryWrapper<User> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(User::getUsername, dto.getUsername());
        Long count = userMapper.selectCount(wrapper);
        if (count > 0) {
            throw new BusinessException(400, "用户名已存在");
        }

        // 2. 检查邮箱是否已存在
        LambdaQueryWrapper<User> emailWrapper = new LambdaQueryWrapper<>();
        emailWrapper.eq(User::getEmail, dto.getEmail());
        Long emailCount = userMapper.selectCount(emailWrapper);
        if (emailCount > 0) {
            throw new BusinessException(400, "邮箱已被注册");
        }

        // 3. 构建用户
        User user = new User();
        user.setUsername(dto.getUsername());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(StringUtils.hasText(dto.getNickname()) ? dto.getNickname() : dto.getUsername());
        user.setEmail(dto.getEmail());
        user.setPhone(dto.getPhone());
        user.setRole(StringUtils.hasText(dto.getRole()) ? dto.getRole() : "STUDENT");
        user.setStatus(1);

        // 4. 插入数据库
        userMapper.insert(user);
        log.info("管理员新增用户成功: {}", dto.getUsername());

        return toUserVO(user);
    }
}
