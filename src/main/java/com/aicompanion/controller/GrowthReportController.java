package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.vo.AiReportAnalysis;
import com.aicompanion.model.vo.WeeklyReportVO;
import com.aicompanion.service.GrowthReportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 成长周报控制器
 */
@Tag(name = "成长周报", description = "AI 驱动的学习成长周报接口")
@RestController
@RequestMapping("/report")
@RequiredArgsConstructor
public class GrowthReportController {

    private final GrowthReportService growthReportService;

    /**
     * 获取成长周报统计数据（快速返回，不含 AI 分析）
     */
    @Operation(summary = "获取成长周报统计数据", description = "学习时长、技能进度、学习活动等统计数据")
    @GetMapping("/stats")
    public Result<WeeklyReportVO> getReportStats(
            @RequestParam(defaultValue = "week") String type) {
        Long userId = SecurityUtil.getCurrentUserId();
        WeeklyReportVO report = growthReportService.generateReportStats(userId, type);
        return Result.success("获取成功", report);
    }

    /**
     * 获取 AI 成长分析（独立调用 Spring AI，较慢）
     */
    @Operation(summary = "获取 AI 成长分析", description = "基于统计数据 + Spring AI 生成个性化成长分析")
    @GetMapping("/ai-analysis")
    public Result<AiReportAnalysis> getAiAnalysis(
            @RequestParam(defaultValue = "week") String type) {
        Long userId = SecurityUtil.getCurrentUserId();
        AiReportAnalysis analysis = growthReportService.generateAiAnalysis(userId, type);
        return Result.success("获取成功", analysis);
    }
}
