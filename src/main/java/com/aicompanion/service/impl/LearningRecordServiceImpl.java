package com.aicompanion.service.impl;

import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.model.vo.LearningStatsVO;
import com.aicompanion.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 学习记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;

    @Override
    public LearningStatsVO getUserLearningStats(Long userId) {
        LearningStatsVO stats = learningRecordMapper.getUserLearningStats(userId);
        if (stats == null) {
            stats = new LearningStatsVO();
        }
        
        // 暂时设置为0，后续可以添加学习计划的统计
        stats.setActivePlansCount(0);
        Integer consecutiveDays = learningRecordMapper.selectConsecutiveDays(userId);
        stats.setConsecutiveDays(consecutiveDays != null ? consecutiveDays : 0);

        log.info("获取用户学习统计数据成功: userId={}, stats={}", userId, stats);
        return stats;
    }
}
