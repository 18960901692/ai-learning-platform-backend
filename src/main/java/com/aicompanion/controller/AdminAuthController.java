package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.LoginDTO;
import com.aicompanion.model.dto.RefreshTokenDTO;
import com.aicompanion.model.vo.LoginVO;
import com.aicompanion.service.AdminAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员认证控制器
 */
@Tag(name = "管理员认证", description = "管理员登录、登出、刷新令牌相关接口")
@RestController
@RequestMapping("/admin/auth")
@RequiredArgsConstructor
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    @Operation(summary = "管理员登录", description = "管理员使用用户名密码登录")
    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        return Result.success(adminAuthService.login(dto));
    }

    @Operation(summary = "刷新令牌", description = "使用刷新令牌获取新的访问令牌")
    @PostMapping("/refresh")
    public Result<LoginVO> refreshToken(@Valid @RequestBody RefreshTokenDTO dto) {
        return Result.success(adminAuthService.refreshToken(dto));
    }

    @Operation(summary = "管理员登出", description = "将当前 Token 加入黑名单并清除刷新令牌")
    @PostMapping("/logout")
    public Result<Void> logout(@RequestAttribute(required = false) Long userId,
                               jakarta.servlet.http.HttpServletRequest request) {
        String token = com.aicompanion.common.util.SecurityUtil.extractToken(request);
        if (userId != null) {
            adminAuthService.logout(userId, token);
        }
        return Result.success("登出成功", null);
    }
}
