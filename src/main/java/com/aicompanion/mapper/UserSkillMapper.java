package com.aicompanion.mapper;

import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.UserSkillVO;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 用户技能 Mapper
 */
@Mapper
public interface UserSkillMapper extends BaseMapper<UserSkill> {

    /**
     * 获取用户技能掌握情况列表
     */
    @Select("SELECT " +
            "s.id AS skillId, " +
            "s.name AS skillName, " +
            "s.category, " +
            "s.level AS difficultyLevel, " +
            "us.level, " +
            "us.status " +
            "FROM user_skill us " +
            "INNER JOIN skill s ON us.skill_id = s.id " +
            "WHERE us.user_id = #{userId} " +
            "ORDER BY us.update_time DESC")
    List<UserSkillVO> getUserSkills(@Param("userId") Long userId);
}
