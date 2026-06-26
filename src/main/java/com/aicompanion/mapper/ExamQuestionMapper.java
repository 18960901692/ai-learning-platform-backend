package com.aicompanion.mapper;

import com.aicompanion.model.entity.ExamQuestion;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamQuestionMapper extends BaseMapper<ExamQuestion> {

    @Select("SELECT * FROM exam_question WHERE skill_id = #{skillId} ORDER BY RAND() LIMIT #{limit}")
    List<ExamQuestion> findRandomBySkillId(@Param("skillId") Long skillId, @Param("limit") int limit);

    @Select("SELECT COUNT(*) FROM exam_question WHERE skill_id = #{skillId}")
    int countBySkillId(@Param("skillId") Long skillId);
}
