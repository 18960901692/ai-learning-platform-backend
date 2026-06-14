package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.UserDTO;
import com.aicompanion.model.vo.UserVO;
import com.aicompanion.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器（获取/更新用户信息）
 */
@Tag(name = "用户管理", description = "用户信息查询与修改相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     */
    @Operation(summary = "获取当前用户信息", description = "获取当前登录用户的详细信息")
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    /**
     * 根据 ID 获取用户信息
     */
    @Operation(summary = "根据ID获取用户信息", description = "根据用户ID获取用户详细信息")
    @GetMapping("/{id}")
    public Result<UserVO> getUserById(@PathVariable Long id) {
        UserVO userVO = userService.getUserInfo(id);
        return Result.success(userVO);
    }

    /**
     * 更新用户信息
     */
    @Operation(summary = "更新用户信息", description = "更新当前用户的个人信息")
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UserDTO dto,
                                     HttpServletRequest request) {
        // 校验权限：只能修改自己的信息（管理员除外）
        Long currentUserId = SecurityUtil.getCurrentUserId(request);
        if (!currentUserId.equals(id) && !SecurityUtil.isAdmin(request)) {
            throw new RuntimeException("无权修改他人信息");
        }
        UserVO userVO = userService.updateUser(id, dto);
        return Result.success("更新成功", userVO);
    }
}
