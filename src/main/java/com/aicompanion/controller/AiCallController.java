package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.service.AiCallLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

/**
 * AI 调用统计控制器
 */
@Tag(name = "AI调用统计", description = "管理员查看 AI 调用次数统计")
@RestController
@RequestMapping("/ai-call")
@RequiredArgsConstructor
public class AiCallController {

    private final AiCallLogService aiCallLogService;

    /**
     * 按天统计近 N 天的调用次数（按类型分组）
     */
    @Operation(summary = "按天统计调用次数", description = "获取近 N 天每天的 AI 调用次数，按调用类型分组")
    @GetMapping("/statistics/daily")
    public Result<List<Map<String, Object>>> countDailyByType(int days) {
        List<Map<String, Object>> result = aiCallLogService.countDailyByType(days);
        return Result.success(result);
    }

    /**
     * 统计总调用次数和成功次数
     */
    @Operation(summary = "总调用统计", description = "获取总调用次数、成功次数、成功率等")
    @GetMapping("/statistics/total")
    public Result<Map<String, Object>> countTotal() {
        Map<String, Object> result = aiCallLogService.countTotal();
        return Result.success(result);
    }

    /**
     * 按类型统计调用次数
     */
    @Operation(summary = "按类型统计", description = "获取各调用类型的次数分布")
    @GetMapping("/statistics/type-distribution")
    public Result<List<Map<String, Object>>> countByType() {
        List<Map<String, Object>> result = aiCallLogService.countByType();
        return Result.success(result);
    }

    /**
     * 调用次数最多的用户 TOP10
     */
    @Operation(summary = "用户排行榜", description = "获取 AI 调用次数最多的 TOP10 用户")
    @GetMapping("/statistics/top-users")
    public Result<List<Map<String, Object>>> topUsers() {
        List<Map<String, Object>> result = aiCallLogService.topUsers();
        return Result.success(result);
    }
}
