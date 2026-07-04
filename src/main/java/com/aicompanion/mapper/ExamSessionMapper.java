package com.aicompanion.mapper;

import com.aicompanion.model.entity.ExamSession;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ExamSessionMapper extends BaseMapper<ExamSession> {

    /**
     * 清理僵尸考核记录：将超时的 IN_PROGRESS 记录标记为 ABANDONED
     * @param hours 超时小时数
     * @return 清理记录数
     */
    @Update("UPDATE exam_session SET status = 'ABANDONED', end_time = NOW() " +
            "WHERE status = 'IN_PROGRESS' AND start_time < DATE_SUB(NOW(), INTERVAL #{hours} HOUR)")
    int cleanAbandonedExams(int hours);
}
