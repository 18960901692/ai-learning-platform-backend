package com.aicompanion.service;

import com.aicompanion.model.vo.LearningStatsVO;

/**
 * 学习记录服务接口
 */
public interface LearningRecordService {

    /**
     * 获取用户学习统计数据
     */
    LearningStatsVO getUserLearningStats(Long userId);
}
