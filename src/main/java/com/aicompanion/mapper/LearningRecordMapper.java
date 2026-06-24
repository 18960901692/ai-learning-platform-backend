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
}
