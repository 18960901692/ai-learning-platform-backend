package com.aicompanion.controller;

import com.aicompanion.common.Result;
import com.aicompanion.dto.UserDTO;
import com.aicompanion.service.UserService;
import com.aicompanion.vo.UserVO;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 用户控制器（获取/更新用户信息）
 */
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    /**
     * 获取当前登录用户信息
     * GET /api/users/me
     */
    @GetMapping("/me")
    public Result<UserVO> getCurrentUser(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute("userId");
        UserVO userVO = userService.getUserInfo(userId);
        return Result.success(userVO);
    }

    /**
     * 更新用户信息
     * PUT /api/users/{id}
     */
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id,
                                     @Valid @RequestBody UserDTO dto) {
        UserVO userVO = userService.updateUser(id, dto);
        return Result.success("更新成功", userVO);
    }
}
