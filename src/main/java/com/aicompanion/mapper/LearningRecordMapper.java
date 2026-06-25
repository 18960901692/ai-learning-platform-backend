package com.aicompanion.mapper;

import com.aicompanion.model.entity.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

/**
 * 学习记录 Mapper
 */
@Mapper
public interface LearningRecordMapper extends BaseMapper<LearningRecord> {

    /**
     * 获取用户学习统计
     */
    @Select("SELECT " +
            "COALESCE(SUM(study_seconds), 0) AS totalStudySeconds, " +
            "SUM(CASE WHEN status = 1 THEN 1 ELSE 0 END) AS studyingSkillsCount, " +
            "SUM(CASE WHEN status = 2 THEN 1 ELSE 0 END) AS completedSkillsCount " +
            "FROM learning_record WHERE user_id = #{userId}")
    com.aicompanion.model.vo.LearningStatsVO getUserLearningStats(@Param("userId") Long userId);

    /**
     * 查询用户连续打卡天数
     * 从最近一次学习记录开始，向前查找连续有学习记录的天数
     */
    @Select("SELECT COUNT(DISTINCT DATE(created_at)) as consecutive_days " +
            "FROM ai_learning_record " +
            "WHERE user_id = #{userId} AND deleted = 0 " +
            "AND DATE(created_at) >= DATE_SUB(CURDATE(), INTERVAL 30 DAY) " +
            "ORDER BY DATE(created_at) DESC")
    Integer selectConsecutiveDays(@Param("userId") Long userId);
}
