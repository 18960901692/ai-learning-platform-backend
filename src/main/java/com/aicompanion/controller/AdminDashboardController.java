package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.model.vo.DashboardStatsVO;
import com.aicompanion.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台首页统计 Controller
 */
@RestController
@RequestMapping("/admin/dashboard")
@RequiredArgsConstructor
public class AdminDashboardController {

    private final AdminDashboardService dashboardService;

    /**
     * 获取首页统计数据
     *
     * @param days 用户增长趋势天数，默认 30
     */
    @GetMapping("/stats")
    public Result<DashboardStatsVO> getStats(
            @RequestParam(defaultValue = "30") int days) {
        return Result.success(dashboardService.getDashboardStats(days));
    }
}