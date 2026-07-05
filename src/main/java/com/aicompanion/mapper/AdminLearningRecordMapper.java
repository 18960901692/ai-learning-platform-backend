package com.aicompanion.mapper;

import com.aicompanion.model.vo.AdminLearningRecordVO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 管理后台学习记录 Mapper
 */
@Mapper
public interface AdminLearningRecordMapper {

    /**
     * 分页查询所有用户的学习记录（关联用户名，支持状态筛选）
     */
    @Select({
        "<script>",
        "SELECT lr.id, lr.user_id, u.username, u.nickname, lr.skill_id, lr.skill_name, ",
        "lr.progress, lr.study_seconds, lr.status, lr.last_study_time, lr.create_time ",
        "FROM learning_record lr ",
        "LEFT JOIN user u ON lr.user_id = u.id ",
        "WHERE lr.deleted = 0 ",
        "<if test='status != null'>",
        "  AND lr.status = #{status} ",
        "</if>",
        "AND (#{keyword} IS NULL OR #{keyword} = '' OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%')) ",
        "ORDER BY lr.last_study_time DESC ",
        "LIMIT #{offset}, #{pageSize}",
        "</script>"
    })
    List<AdminLearningRecordVO> selectLearningRecords(@Param("keyword") String keyword,
                                                       @Param("status") Integer status,
                                                       @Param("offset") int offset,
                                                       @Param("pageSize") int pageSize);

    /**
     * 统计学习记录总数（支持状态筛选）
     */
    @Select({
        "<script>",
        "SELECT COUNT(*) FROM learning_record lr ",
        "LEFT JOIN user u ON lr.user_id = u.id ",
        "WHERE lr.deleted = 0 ",
        "<if test='status != null'>",
        "  AND lr.status = #{status} ",
        "</if>",
        "AND (#{keyword} IS NULL OR #{keyword} = '' OR u.username LIKE CONCAT('%', #{keyword}, '%') OR u.nickname LIKE CONCAT('%', #{keyword}, '%'))",
        "</script>"
    })
    long countLearningRecords(@Param("keyword") String keyword, @Param("status") Integer status);
}
