package com.aicompanion.service.impl;

import com.aicompanion.mapper.AdminDashboardMapper;
import com.aicompanion.model.vo.DashboardStatsVO;
import com.aicompanion.service.AdminDashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 管理后台首页统计 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminDashboardServiceImpl implements AdminDashboardService {

    private final AdminDashboardMapper dashboardMapper;

    @Override
    public DashboardStatsVO getDashboardStats(int days) {
        DashboardStatsVO vo = new DashboardStatsVO();

        vo.setTotalUsers(dashboardMapper.countTotalUsers());
        vo.setTotalLitSkills(dashboardMapper.countTotalLitSkills());
        vo.setTodayActiveUsers(dashboardMapper.countTodayActiveUsers());
        vo.setTodayAiCalls(dashboardMapper.countTodayAiCalls());

        vo.setUserGrowth(dashboardMapper.userGrowthTrend(days));
        vo.setRecentLearners(dashboardMapper.recentLearners());
        vo.setRecentExams(dashboardMapper.recentExams());

        log.info("首页统计: 用户总数={}, 点亮数={}, 今日活跃={}, 今日AI调用={}",
                vo.getTotalUsers(), vo.getTotalLitSkills(),
                vo.getTodayActiveUsers(), vo.getTodayAiCalls());

        return vo;
    }
}