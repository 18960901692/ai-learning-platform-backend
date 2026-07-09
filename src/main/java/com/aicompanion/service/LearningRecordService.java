package com.aicompanion.service;

import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.model.vo.LearningStatsVO;

import java.util.List;

/**
 * 学习记录服务接口
 */
public interface LearningRecordService {

    /**
     * 获取用户学习统计数据
     */
    LearningStatsVO getUserLearningStats(Long userId);

    /**
     * 获取用户学习记录列表
     */
    List<LearningRecordVO> getUserLearningRecords(Long userId);

    /**
     * 开始学习某技能，返回 learning_record.id
     */
    Long startLearning(Long userId, Long skillId);

    /**
     * 学习心跳，累加 30 秒
     */
    void heartbeat(Long recordId);

    /**
     * 结束学习，结算时长
     * @param recordId 学习记录ID
     * @param clientStudySeconds 前端本地计时的时长（秒）
     */
    LearningRecordVO endLearning(Long recordId, Integer clientStudySeconds);

    /**
     * 批量刷盘 Redis 心跳缓冲到 MySQL（由定时任务调用）
     * @return 成功刷盘的记录数
     */
    int flushBufferedHeartbeats();
}
