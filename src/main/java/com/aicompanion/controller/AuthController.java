package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.dto.RegisterDTO;
import com.aicompanion.model.vo.LoginVO;
import com.aicompanion.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 认证控制器（注册、登录、刷新令牌）
 */
@Tag(name = "认证管理", description = "用户注册、登录、刷新令牌相关接口")
@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final UserService userService;

    /**
     * 用户注册
     */
    @Operation(summary = "用户注册", description = "注册新用户账号")
    @PostMapping("/register")
    public Result<Void> register(@Valid @RequestBody RegisterDTO dto) {
        userService.register(dto);
        return Result.success("注册成功", null);
    }

    /**
     * 用户登录
     */
    @Operation(summary = "用户登录", description = "账号密码登录，支持记住密码功能")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        LoginVO loginVO = userService.login(dto);
        return Result.success("登录成功", loginVO);
    }

    /**
     * 刷新令牌（记住密码免登录）
     */
    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌（记住密码免登录）")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        LoginVO loginVO = userService.refreshToken(dto);
        return Result.success("刷新成功", loginVO);
    }

    /**
     * 退出登录
     * 将当前 Token 加入黑名单，实现主动失效
     */
    @Operation(summary = "退出登录", description = "将当前 Token 加入黑名单并清除刷新令牌，退出登录状态")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute(required = false) Long userId,
                               jakarta.servlet.http.HttpServletRequest request) {
        String token = com.aicompanion.common.util.SecurityUtil.extractToken(request);
        userService.logout(userId, token);
        return Result.success("退出成功", null);
    }
}
