package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.ChatMessageMapper;
import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.model.dto.CreateUserDTO;
import com.aicompanion.model.dto.UpdateUserDTO;
import com.aicompanion.model.dto.UserDTO;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.model.vo.AdminUserProfileVO;
import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.model.vo.LearningStatsVO;
import com.aicompanion.model.vo.UserVO;
import com.aicompanion.service.LearningRecordService;
import com.aicompanion.service.UserService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
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
    private final LearningRecordMapper learningRecordMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;

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

    /**
     * 删除用户（管理员）
     */
    @Operation(summary = "删除用户", description = "管理员删除指定用户（禁止删除管理员）")
    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        userService.deleteUser(id);
        return Result.success("删除成功", null);
    }

    /**
     * 获取用户学习画像（管理员）
     */
    @Operation(summary = "获取用户学习画像", description = "管理员查看指定用户的学习时长、掌握技能数、连续打卡天数、AI对话次数等")
    @GetMapping("/{id}/profile")
    public Result<AdminUserProfileVO> getUserProfile(@PathVariable Long id) {
        UserVO user = userService.getUserInfo(id);
        if (user == null) {
            return Result.fail("用户不存在");
        }

        AdminUserProfileVO profile = new AdminUserProfileVO();
        profile.setUserId(user.getId());
        profile.setUsername(user.getUsername());
        profile.setNickname(user.getNickname());
        profile.setEmail(user.getEmail());
        profile.setPhone(user.getPhone());
        profile.setRole(user.getRole());
        profile.setCreateTime(user.getCreateTime());

        // 学习统计
        LearningStatsVO stats = learningRecordMapper.getUserLearningStats(id);
        if (stats != null) {
            profile.setTotalStudySeconds(stats.getTotalStudySeconds());
            profile.setStudyingSkillsCount(stats.getStudyingSkillsCount());
            profile.setCompletedSkillsCount(stats.getCompletedSkillsCount());
        } else {
            profile.setTotalStudySeconds(0);
            profile.setStudyingSkillsCount(0);
            profile.setCompletedSkillsCount(0);
        }

        // 连续打卡天数
        Integer consecutiveDays = learningRecordMapper.selectConsecutiveDays(id);
        profile.setConsecutiveDays(consecutiveDays != null ? consecutiveDays : 0);

        // AI 对话统计
        LambdaQueryWrapper<ChatSession> chatWrapper = new LambdaQueryWrapper<>();
        chatWrapper.eq(ChatSession::getUserId, id)
                   .eq(ChatSession::getAgentType, "CHAT");
        Long chatCount = chatSessionMapper.selectCount(chatWrapper);
        profile.setAiChatCount(chatCount != null ? chatCount.intValue() : 0);

        LambdaQueryWrapper<ChatSession> interviewWrapper = new LambdaQueryWrapper<>();
        interviewWrapper.eq(ChatSession::getUserId, id)
                        .eq(ChatSession::getAgentType, "INTERVIEW");
        Long interviewCount = chatSessionMapper.selectCount(interviewWrapper);
        profile.setAiInterviewCount(interviewCount != null ? interviewCount.intValue() : 0);

        return Result.success(profile);
    }
}