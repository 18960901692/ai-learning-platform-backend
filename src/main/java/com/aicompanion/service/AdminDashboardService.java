package com.aicompanion.service;

import com.aicompanion.model.vo.DashboardStatsVO;

/**
 * 管理后台首页统计 Service
 */
public interface AdminDashboardService {

    /**
     * 获取首页统计数据
     *
     * @param days 用户增长趋势天数，默认 30
     */
    DashboardStatsVO getDashboardStats(int days);
}