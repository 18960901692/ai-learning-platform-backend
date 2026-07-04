package com.aicompanion.mapper;

import com.aicompanion.model.vo.AdminExamRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理后台考核记录 Mapper
 */
@Mapper
public interface AdminExamRecordMapper {

    /**
     * 分页查询考核记录(支持按用户名搜索，仅展示已完成的考核)
     */
    @Select({
        "<script>",
        "SELECT ",
        "  es.id, ",
        "  u.username, ",
        "  s.name AS skill_name, ",
        "  es.total_score AS score, ",
        "  CASE ",
        "    WHEN es.status = 'PASSED' THEN '通过' ",
        "    WHEN es.status = 'FAILED' THEN '未通过' ",
        "    ELSE '未通过' ",
        "  END AS status, ",
        "  CONCAT( ",
        "    TIMESTAMPDIFF(MINUTE, es.start_time, es.end_time), '分钟' ",
        "  ) AS duration, ",
        "  es.end_time ",
        "FROM exam_session es ",
        "JOIN user u ON es.user_id = u.id ",
        "JOIN skill s ON es.skill_id = s.id ",
        "WHERE es.deleted = 0 AND u.deleted = 0 ",
        "  AND es.status IN ('PASSED', 'FAILED') ",
        "<if test='keyword != null and keyword != \"\"'>",
        "  AND u.username LIKE CONCAT('%', #{keyword}, '%') ",
        "</if>",
        "ORDER BY es.end_time DESC ",
        "LIMIT #{offset}, #{pageSize}",
        "</script>"
    })
    List<AdminExamRecordVO> selectExamRecords(
        @Param("keyword") String keyword,
        @Param("offset") int offset,
        @Param("pageSize") int pageSize
    );

    /**
     * 查询考核记录总数(支持按用户名搜索，仅统计已完成的考核)
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) ",
        "FROM exam_session es ",
        "JOIN user u ON es.user_id = u.id ",
        "WHERE es.deleted = 0 AND u.deleted = 0 ",
        "  AND es.status IN ('PASSED', 'FAILED') ",
        "<if test='keyword != null and keyword != \"\"'>",
        "  AND u.username LIKE CONCAT('%', #{keyword}, '%') ",
        "</if>",
        "</script>"
    })
    long countExamRecords(@Param("keyword") String keyword);
}
