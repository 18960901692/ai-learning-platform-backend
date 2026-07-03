package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.CreateUserDTO;
import com.aicompanion.model.dto.UpdateUserDTO;
import com.aicompanion.model.dto.UserDTO;
import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.model.vo.LearningStatsVO;
import com.aicompanion.model.vo.UserVO;
import com.aicompanion.service.LearningRecordService;
import com.aicompanion.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 用户控制器（获取/更新用户信息）
 */
@Tag(name = "用户管理", description = "用户信息查询与修改相关接口")
@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;
    private final LearningRecordService learningRecordService;

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
     * 获取当前用户学习统计
     */
    @Operation(summary = "获取用户学习统计", description = "获取当前用户的学习统计数据")
    @GetMapping("/me/stats")
    public Result<LearningStatsVO> getUserLearningStats(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);
        LearningStatsVO stats = learningRecordService.getUserLearningStats(userId);
        return Result.success("获取成功", stats);
    }

    /**
     * 获取当前用户学习记录列表
     */
    @Operation(summary = "获取用户学习记录列表", description = "获取当前用户的所有学习记录")
    @GetMapping("/me/records")
    public Result<List<LearningRecordVO>> getUserLearningRecords(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);
        List<LearningRecordVO> records = learningRecordService.getUserLearningRecords(userId);
        return Result.success("获取成功", records);
    }

    /**
     * 分页查询用户列表（管理员）
     */
    @Operation(summary = "分页查询用户列表", description = "管理员分页查询所有用户，支持按用户名/昵称搜索")
    @GetMapping("/list")
    public Result<PageResult<UserVO>> getUserList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(userService.getUserList(page, pageSize, keyword));
    }

    /**
     * 动态搜索用户（XML Mapper 实现）
     */
    @Operation(summary = "动态搜索用户", description = "支持按角色筛选 + 关键词模糊搜索（用户名/昵称/邮箱）")
    @GetMapping("/search")
    public Result<List<UserVO>> searchUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String role) {
        return Result.success(userService.searchUsers(keyword, role));
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
     * 新增用户（管理员）
     */
    @Operation(summary = "新增用户", description = "管理员创建新用户")
    @PostMapping
    public Result<UserVO> createUser(@Valid @RequestBody CreateUserDTO dto) {
        UserVO userVO = userService.createUser(dto);
        return Result.success("新增成功", userVO);
    }

    @Operation(summary = "修改当前用户信息", description = "当前登录用户修改自己的信息")
    @PutMapping("/me")
    public Result<UserVO> updateCurrentUser(@Valid @RequestBody UpdateUserDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        UserVO userVO = userService.updateUserById(userId, dto);
        return Result.success("修改成功", userVO);
    }

    @Operation(summary = "修改用户信息", description = "管理员修改指定用户的信息")
    @PutMapping("/{id}")
    public Result<UserVO> updateUser(@PathVariable Long id, @Valid @RequestBody UpdateUserDTO dto) {
        UserVO userVO = userService.updateUserById(id, dto);
        return Result.success("修改成功", userVO);
    }

    @Operation(summary = "删除用户", description = "管理员删除指定用户（禁止删除管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }
}
