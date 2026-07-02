package com.aicompanion.mapper;

import com.aicompanion.model.entity.AiCallLog;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface AiCallLogMapper extends BaseMapper<AiCallLog> {

    /**
     * 按天统计近 N 天的调用次数（按类型分组）
     */
    @Select("""
        SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, call_type, COUNT(*) AS cnt
        FROM ai_call_log
        WHERE create_time >= #{startDate} AND success = 1
        GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d'), call_type
        ORDER BY date
        """)
    List<Map<String, Object>> countDailyByType(String startDate);

    /**
     * 统计总调用次数和成功次数
     */
    @Select("""
        SELECT COUNT(*) AS totalCalls,
               SUM(CASE WHEN success = 1 THEN 1 ELSE 0 END) AS successCalls
        FROM ai_call_log
        """)
    Map<String, Object> countTotal();

    /**
     * 按类型统计调用次数
     */
    @Select("""
        SELECT call_type, COUNT(*) AS cnt
        FROM ai_call_log
        WHERE success = 1
        GROUP BY call_type
        ORDER BY cnt DESC
        """)
    List<Map<String, Object>> countByType();

    /**
     * 调用次数最多的用户 TOP10
     */
    @Select("""
        SELECT u.id, u.username, u.nickname, COUNT(*) AS cnt
        FROM ai_call_log a
        JOIN user u ON a.user_id = u.id
        WHERE a.success = 1
        GROUP BY a.user_id, u.username, u.nickname
        ORDER BY cnt DESC
        LIMIT 10
        """)
    List<Map<String, Object>> topUsers();
}
