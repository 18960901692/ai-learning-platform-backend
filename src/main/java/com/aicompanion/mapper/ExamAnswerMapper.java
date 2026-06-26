package com.aicompanion.mapper;

import com.aicompanion.model.entity.ExamAnswer;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ExamAnswerMapper extends BaseMapper<ExamAnswer> {

    @Select("SELECT * FROM exam_answer WHERE session_id = #{sessionId} ORDER BY question_order ASC")
    List<ExamAnswer> findBySessionId(@Param("sessionId") Long sessionId);
}
