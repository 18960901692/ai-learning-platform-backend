package com.aicompanion.mapper;

import com.aicompanion.model.entity.LearningRecord;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

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
            "FROM learning_record WHERE user_id = #{userId} AND deleted = 0")
    com.aicompanion.model.vo.LearningStatsVO getUserLearningStats(@Param("userId") Long userId);

    /**
     * 查询用户连续打卡天数
     * 从最近一次学习记录开始，向前查找连续有学习记录的天数
     * 使用 last_study_time 而非 create_time，因为同技能学习多天时 create_time 不会更新
     */
    @Select("SELECT COUNT(*) as consecutive_days " +
            "FROM ( " +
            "    SELECT @rn := @rn + 1 as rn, study_date " +
            "    FROM ( " +
            "        SELECT DISTINCT DATE(last_study_time) as study_date " +
            "        FROM learning_record " +
            "        WHERE user_id = #{userId} AND deleted = 0 " +
            "        ORDER BY study_date DESC " +
            "    ) dates, (SELECT @rn := 0) r " +
            ") t " +
            "WHERE study_date = DATE_SUB(CURDATE(), INTERVAL (rn - 1) DAY)")
    Integer selectConsecutiveDays(@Param("userId") Long userId);

    /**
     * 获取用户学习记录列表（按最近学习时间倒序）
     */
    @Select("SELECT * FROM learning_record " +
            "WHERE user_id = #{userId} " +
            "ORDER BY last_study_time DESC, create_time DESC")
    List<LearningRecord> selectByUserId(@Param("userId") Long userId);
}
