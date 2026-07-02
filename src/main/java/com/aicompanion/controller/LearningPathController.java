package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.vo.LearningPathVO;
import com.aicompanion.service.LearningPathService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 学习路径推荐控制器
 */
@Tag(name = "学习路径推荐", description = "AI个性化学习路径推荐接口")
@RestController
@RequestMapping("/learning-path")
@RequiredArgsConstructor
public class LearningPathController {

    private final LearningPathService learningPathService;

    /**
     * 获取推荐学习路径（纯规则）
     */
    @Operation(summary = "获取推荐学习路径", description = "基于规则引擎生成个性化学习路径推荐")
    @GetMapping("/recommend")
    public Result<List<LearningPathVO>> getRecommendations() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<LearningPathVO> recommendations = learningPathService.getRecommendations(userId);
        return Result.success(recommendations);
    }

    /**
     * 获取推荐学习路径（规则 + AI增强）
     */
    @Operation(summary = "获取推荐学习路径（AI增强）", description = "基于规则引擎 + Spring AI生成个性化推荐理由")
    @GetMapping("/recommend/ai")
    public Result<List<LearningPathVO>> getRecommendationsWithAi() {
        Long userId = SecurityUtil.getCurrentUserId();
        List<LearningPathVO> recommendations = learningPathService.getRecommendationsWithAi(userId);
        return Result.success(recommendations);
    }
}
