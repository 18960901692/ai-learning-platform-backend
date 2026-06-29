package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.SkillDTO;
import com.aicompanion.model.vo.SkillVO;
import com.aicompanion.model.vo.UserSkillVO;
import com.aicompanion.service.SkillService;
import com.aicompanion.service.UserSkillService;
import com.aicompanion.common.util.SecurityUtil;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 技能控制器
 */
@Tag(name = "技能管理", description = "技能增删改查相关接口")
@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillController {

    private final SkillService skillService;
    private final UserSkillService userSkillService;

    @Operation(summary = "新增技能", description = "管理员创建新技能")
    @PostMapping
    public Result<SkillVO> createSkill(@Valid @RequestBody SkillDTO dto) {
        return Result.success("新增成功", skillService.createSkill(dto));
    }

    @Operation(summary = "更新技能", description = "管理员更新技能信息")
    @PutMapping("/{id}")
    public Result<SkillVO> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillDTO dto) {
        return Result.success("更新成功", skillService.updateSkill(id, dto));
    }

    @Operation(summary = "删除技能", description = "管理员删除技能")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSkill(@PathVariable Long id) {
        skillService.deleteSkill(id);
        return Result.success("删除成功", null);
    }

    @Operation(summary = "根据ID获取技能", description = "获取指定技能的详细信息")
    @GetMapping("/{id}")
    public Result<SkillVO> getSkillById(@PathVariable Long id) {
        return Result.success(skillService.getSkillById(id));
    }

    @Operation(summary = "分页查询技能列表", description = "分页查询所有技能，支持按分类筛选和关键词搜索")
    @GetMapping("/list")
    public Result<PageResult<SkillVO>> getSkillList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String keyword) {
        return Result.success(skillService.getSkillList(page, pageSize, category, keyword));
    }

    @Operation(summary = "获取所有技能", description = "获取所有技能列表（不分页）")
    @GetMapping("/all")
    public Result<List<SkillVO>> getAllSkills() {
        return Result.success(skillService.getAllSkills());
    }

    @Operation(summary = "获取所有分类", description = "获取所有技能分类列表")
    @GetMapping("/categories")
    public Result<List<String>> getAllCategories() {
        return Result.success(skillService.getAllCategories());
    }

    @Operation(summary = "获取技能树", description = "获取树形结构的技能列表（包含当前用户学习状态）")
    @GetMapping("/tree")
    public Result<List<SkillVO>> getSkillTree(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);
        return Result.success(skillService.getSkillTree(userId));
    }

    @Operation(summary = "获取用户技能掌握情况", description = "获取当前用户的技能掌握情况列表")
    @GetMapping("/user/my-skills")
    public Result<List<UserSkillVO>> getUserSkills(HttpServletRequest request) {
        Long userId = SecurityUtil.getCurrentUserId(request);
        List<UserSkillVO> skills = userSkillService.getUserSkills(userId);
        return Result.success("获取成功", skills);
    }
}
