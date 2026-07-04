package com.aicompanion.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

/**
 * 管理后台首页统计 Mapper
 */
@Mapper
public interface AdminDashboardMapper {

    /**
     * 用户总数
     */
    @Select("SELECT COUNT(*) FROM user WHERE deleted = 0")
    Long countTotalUsers();

    /**
     * 技能总点亮数（user_skill status=2）
     */
    @Select("SELECT COUNT(*) FROM user_skill WHERE status = 2 AND deleted = 0")
    Long countTotalLitSkills();

    /**
     * 今日活跃用户数（今天有学习记录的用户）
     */
    @Select("SELECT COUNT(DISTINCT user_id) FROM learning_record " +
            "WHERE DATE(last_study_time) = CURDATE() AND deleted = 0")
    Long countTodayActiveUsers();

    /**
     * 今日 AI 调用次数
     */
    @Select("SELECT COUNT(*) FROM ai_call_log WHERE DATE(create_time) = CURDATE() AND success = 1")
    Long countTodayAiCalls();

    /**
     * 近 N 天用户增长趋势（按注册日期分组）
     */
    @Select("SELECT DATE_FORMAT(create_time, '%Y-%m-%d') AS date, COUNT(*) AS cnt " +
            "FROM user WHERE deleted = 0 AND create_time >= DATE_SUB(CURDATE(), INTERVAL #{days} DAY) " +
            "GROUP BY DATE_FORMAT(create_time, '%Y-%m-%d') ORDER BY date")
    List<Map<String, Object>> userGrowthTrend(int days);

    /**
     * 最近活跃学习用户 Top 5（每个用户只显示最近一条学习记录）
     */
    @Select("SELECT u.id, u.username, u.nickname, " +
            "(SELECT s.name FROM learning_record lr2 " +
            " JOIN skill s ON lr2.skill_id = s.id " +
            " WHERE lr2.user_id = u.id AND lr2.deleted = 0 " +
            " ORDER BY lr2.last_study_time DESC LIMIT 1) AS skill_name, " +
            "(SELECT lr2.progress FROM learning_record lr2 " +
            " WHERE lr2.user_id = u.id AND lr2.deleted = 0 " +
            " ORDER BY lr2.last_study_time DESC LIMIT 1) AS progress, " +
            "(SELECT lr2.study_seconds FROM learning_record lr2 " +
            " WHERE lr2.user_id = u.id AND lr2.deleted = 0 " +
            " ORDER BY lr2.last_study_time DESC LIMIT 1) AS study_seconds, " +
            "(SELECT lr2.status FROM learning_record lr2 " +
            " WHERE lr2.user_id = u.id AND lr2.deleted = 0 " +
            " ORDER BY lr2.last_study_time DESC LIMIT 1) AS status, " +
            "DATE_FORMAT(" +
            "  (SELECT MAX(lr3.last_study_time) FROM learning_record lr3 " +
            "   WHERE lr3.user_id = u.id AND lr3.deleted = 0), " +
            "  '%Y-%m-%d %H:%i:%s' " +
            ") AS last_study_time " +
            "FROM user u " +
            "WHERE u.deleted = 0 " +
            "AND EXISTS (SELECT 1 FROM learning_record lr4 WHERE lr4.user_id = u.id AND lr4.deleted = 0) " +
            "ORDER BY last_study_time DESC LIMIT 5")
    List<Map<String, Object>> recentLearners();

    /**
     * 最近考核情况 Top 5（仅展示已完成的考核：通过/未通过）
     */
    @Select("SELECT es.id, u.username, u.nickname, s.name AS skill_name, " +
            "es.total_score, es.pass_score, es.status, " +
            "DATE_FORMAT(es.start_time, '%Y-%m-%d %H:%i:%s') AS start_time " +
            "FROM exam_session es " +
            "JOIN user u ON es.user_id = u.id " +
            "JOIN skill s ON es.skill_id = s.id " +
            "WHERE es.deleted = 0 AND u.deleted = 0 " +
            "  AND es.status IN ('PASSED', 'FAILED') " +
            "ORDER BY es.end_time DESC LIMIT 5")
    List<Map<String, Object>> recentExams();
}