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
     * 最近活跃学习用户 Top 5
     */
    @Select("SELECT u.id, u.username, u.nickname, u.avatar, " +
            "s.name AS skill_name, lr.last_study_time " +
            "FROM learning_record lr " +
            "JOIN user u ON lr.user_id = u.id " +
            "JOIN skill s ON lr.skill_id = s.id " +
            "WHERE lr.deleted = 0 AND u.deleted = 0 " +
            "ORDER BY lr.last_study_time DESC LIMIT 5")
    List<Map<String, Object>> recentLearners();

    /**
     * 最近考核情况 Top 5
     */
    @Select("SELECT es.id, u.username, u.nickname, s.name AS skill_name, " +
            "es.total_score, es.pass_score, es.status, es.start_time " +
            "FROM exam_session es " +
            "JOIN user u ON es.user_id = u.id " +
            "JOIN skill s ON es.skill_id = s.id " +
            "WHERE es.deleted = 0 AND u.deleted = 0 " +
            "ORDER BY es.start_time DESC LIMIT 5")
    List<Map<String, Object>> recentExams();
}