package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.model.dto.SkillTreeDTO;
import com.aicompanion.model.vo.SkillTreeVO;
import com.aicompanion.service.SkillTreeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 技能树控制器
 */
@Tag(name = "技能树管理", description = "技能树 CRUD 相关接口")
@RestController
@RequestMapping("/skills")
@RequiredArgsConstructor
public class SkillTreeController {

    private final SkillTreeService skillTreeService;

    @Operation(summary = "分页查询技能列表", description = "支持按名称关键字和分类筛选")
    @GetMapping("/list")
    public Result<PageResult<SkillTreeVO>> getSkillList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category) {
        PageResult<SkillTreeVO> result = skillTreeService.getSkillList(page, pageSize, keyword, category);
        return Result.success(result);
    }

    @Operation(summary = "新增技能", description = "创建新技能")
    @PostMapping
    public Result<SkillTreeVO> createSkill(@Valid @RequestBody SkillTreeDTO dto) {
        SkillTreeVO vo = skillTreeService.createSkill(dto);
        return Result.success("新增成功", vo);
    }

    @Operation(summary = "修改技能", description = "修改指定技能的信息")
    @PutMapping("/{id}")
    public Result<SkillTreeVO> updateSkill(@PathVariable Long id, @Valid @RequestBody SkillTreeDTO dto) {
        SkillTreeVO vo = skillTreeService.updateSkill(id, dto);
        return Result.success("修改成功", vo);
    }

    @Operation(summary = "删除技能", description = "删除指定技能")
    @DeleteMapping("/{id}")
    public Result<Void> deleteSkill(@PathVariable Long id) {
        skillTreeService.deleteSkill(id);
        return Result.success("删除成功", null);
    }
}
