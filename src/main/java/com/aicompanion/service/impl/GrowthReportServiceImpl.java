package com.aicompanion.service.impl;

import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.mapper.ExamSessionMapper;
import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.model.entity.ExamSession;
import com.aicompanion.model.entity.LearningRecord;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.AiReportAnalysis;
import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.model.vo.WeeklyReportVO;
import com.aicompanion.service.GrowthReportService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 成长周报服务实现
 * 方案 A：后端聚合数据塞入 prompt，单次 Spring AI 调用生成分析
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GrowthReportServiceImpl implements GrowthReportService {

    private final LearningRecordMapper learningRecordMapper;
    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;
    private final ChatSessionMapper chatSessionMapper;
    private final ExamSessionMapper examSessionMapper;
    private final ChatClient chatClient;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public WeeklyReportVO generateReportStats(Long userId, String type) {
        log.info("开始生成用户[{}]的{}报统计数据", userId, type);

        // 计算时间范围并聚合统计数据
        TimeRange range = calcTimeRange(type);
        WeeklyReportVO vo = new WeeklyReportVO();
        vo.setType(type);
        vo.setStartDate(range.rangeStart);
        vo.setEndDate(range.rangeEnd);
        aggregateStats(userId, vo, range.rangeStart, range.rangeEnd, range.prevStart, range.prevEnd, range.days);

        log.info("用户[{}]的{}报统计数据生成完成", userId, type);
        return vo;
    }

    @Override
    public AiReportAnalysis generateAiAnalysis(Long userId, String type) {
        log.info("开始生成用户[{}]的{}报AI分析", userId, type);

        // 复用统计数据构造 prompt（避免重复查询）
        WeeklyReportVO vo = generateReportStats(userId, type);
        return generateAiAnalysis(vo);
    }

    /**
     * 计算报告时间范围
     */
    private TimeRange calcTimeRange(String type) {
        LocalDate today = LocalDate.now();
        LocalDate startDate;
        LocalDate prevStartDate;
        LocalDate prevEndDate;
        int days;

        if ("month".equalsIgnoreCase(type)) {
            startDate = today.withDayOfMonth(1);
            prevEndDate = startDate.minusDays(1);
            prevStartDate = prevEndDate.withDayOfMonth(1);
            days = (int) Duration.between(startDate.atStartOfDay(), today.atStartOfDay()).toDays() + 1;
        } else {
            startDate = today.minusDays(6);
            prevEndDate = startDate.minusDays(1);
            prevStartDate = prevEndDate.minusDays(6);
            days = 7;
        }

        TimeRange range = new TimeRange();
        range.rangeStart = startDate.atStartOfDay();
        range.rangeEnd = today.atTime(LocalTime.MAX);
        range.prevStart = prevStartDate.atStartOfDay();
        range.prevEnd = prevEndDate.atTime(LocalTime.MAX);
        range.days = days;
        return range;
    }

    /**
     * 时间范围内部结构
     */
    private static class TimeRange {
        LocalDateTime rangeStart;
        LocalDateTime rangeEnd;
        LocalDateTime prevStart;
        LocalDateTime prevEnd;
        int days;
    }

    /**
     * 聚合各项统计数据
     */
    private void aggregateStats(Long userId, WeeklyReportVO vo,
                                 LocalDateTime rangeStart, LocalDateTime rangeEnd,
                                 LocalDateTime prevStart, LocalDateTime prevEnd,
                                 int days) {
        // 本期学习记录
        List<LearningRecord> currentRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getDeleted, 0)
                .ge(LearningRecord::getCreateTime, rangeStart)
                .le(LearningRecord::getCreateTime, rangeEnd)
        );

        // 本期总学习时长
        int totalSeconds = currentRecords.stream()
            .mapToInt(r -> r.getStudySeconds() != null ? r.getStudySeconds() : 0)
            .sum();
        vo.setTotalStudySeconds(totalSeconds);
        vo.setAvgDailySeconds(days > 0 ? totalSeconds / days : 0);

        // 上期总学习时长（用于对比）
        List<LearningRecord> prevRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getDeleted, 0)
                .ge(LearningRecord::getCreateTime, prevStart)
                .le(LearningRecord::getCreateTime, prevEnd)
        );
        int prevTotal = prevRecords.stream()
            .mapToInt(r -> r.getStudySeconds() != null ? r.getStudySeconds() : 0)
            .sum();
        vo.setPrevTotalStudySeconds(prevTotal);

        // 环比变化百分比
        if (prevTotal > 0) {
            vo.setChangePercent(Math.round(((double) (totalSeconds - prevTotal) / prevTotal) * 1000) / 10.0);
        } else if (totalSeconds > 0) {
            vo.setChangePercent(100.0);
        } else {
            vo.setChangePercent(0.0);
        }

        // 技能进度统计
        List<UserSkill> userSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>().eq(UserSkill::getUserId, userId)
        );
        List<Skill> allSkills = skillMapper.selectList(
            new LambdaQueryWrapper<Skill>().eq(Skill::getDeleted, 0)
        );
        Set<Long> userSkillIds = userSkills.stream()
            .map(UserSkill::getSkillId)
            .collect(Collectors.toSet());

        int mastered = 0, studying = 0;
        for (UserSkill us : userSkills) {
            if (us.getStatus() != null && us.getStatus() == 2) mastered++;
            else if (us.getStatus() != null && us.getStatus() == 1) studying++;
        }
        vo.setMasteredCount(mastered);
        vo.setStudyingCount(studying);
        vo.setNotStartedCount(allSkills.size() - userSkillIds.size());

        // 本期新点亮技能数
        long newMastered = userSkills.stream()
            .filter(us -> us.getStatus() != null && us.getStatus() == 2
                && us.getUpdateTime() != null
                && !us.getUpdateTime().isBefore(rangeStart)
                && !us.getUpdateTime().isAfter(rangeEnd))
            .count();
        vo.setNewMasteredCount((int) newMastered);

        // AI 对话次数（本期 CHAT 类型会话数）
        Long chatCount = chatSessionMapper.selectCount(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getAgentType, "CHAT")
                .ge(ChatSession::getCreateTime, rangeStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .le(ChatSession::getCreateTime, rangeEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );
        vo.setAiChatCount(chatCount != null ? chatCount.intValue() : 0);

        // AI 面试次数（本期 INTERVIEW 类型会话数）
        Long interviewCount = chatSessionMapper.selectCount(
            new LambdaQueryWrapper<ChatSession>()
                .eq(ChatSession::getUserId, userId)
                .eq(ChatSession::getAgentType, "INTERVIEW")
                .ge(ChatSession::getCreateTime, rangeStart.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                .le(ChatSession::getCreateTime, rangeEnd.format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
        );
        vo.setAiInterviewCount(interviewCount != null ? interviewCount.intValue() : 0);

        // 考试次数（本期考试会话数）
        Long examCount = examSessionMapper.selectCount(
            new LambdaQueryWrapper<ExamSession>()
                .eq(ExamSession::getUserId, userId)
                .ge(ExamSession::getCreateTime, rangeStart)
                .le(ExamSession::getCreateTime, rangeEnd)
        );
        vo.setExamCount(examCount != null ? examCount.intValue() : 0);

        // 连续打卡天数
        Integer consecutive = learningRecordMapper.selectConsecutiveDays(userId);
        vo.setConsecutiveDays(consecutive != null ? consecutive : 0);

        // 最近 5 条学习记录
        List<LearningRecord> recent = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getDeleted, 0)
                .ge(LearningRecord::getCreateTime, rangeStart)
                .le(LearningRecord::getCreateTime, rangeEnd)
                .orderByDesc(LearningRecord::getLastStudyTime)
                .last("LIMIT 5")
        );
        List<LearningRecordVO> recentVOs = recent.stream().map(r -> {
            LearningRecordVO v = new LearningRecordVO();
            v.setId(r.getId());
            v.setUserId(r.getUserId());
            v.setSkillId(r.getSkillId());
            v.setSkillName(r.getSkillName());
            v.setSourceType(r.getSourceType());
            v.setProgress(r.getProgress());
            v.setStudySeconds(r.getStudySeconds());
            v.setStatus(r.getStatus());
            v.setFirstStudyTime(r.getFirstStudyTime());
            v.setLastStudyTime(r.getLastStudyTime());
            v.setCompleteTime(r.getCompleteTime());
            v.setCreateTime(r.getCreateTime());
            return v;
        }).collect(Collectors.toList());
        vo.setRecentRecords(recentVOs);
    }

    /**
     * 调用 Spring AI 生成成长分析
     */
    private AiReportAnalysis generateAiAnalysis(WeeklyReportVO vo) {
        String prompt = buildPrompt(vo);

        try {
            String aiResponse = chatClient.prompt()
                .system("""
                    你是一位专业的学习成长导师，擅长根据学生的学习数据分析学习状况。
                    请根据用户的学习数据，生成一份结构化的成长分析报告。
                    严格返回以下 JSON 格式（不要包含 markdown 代码块标记，直接返回纯 JSON）：
                    {
                      "summary": "总体评价，2-3 句话",
                      "strengths": ["做得好的方面，2-3 条"],
                      "weaknesses": ["需要改进的方面，2-3 条"],
                      "suggestions": ["具体可执行的改进建议，3 条"],
                      "nextPlan": ["下周推荐学习重点，3 条"]
                    }
                    要求：
                    1. 内容要结合用户实际数据，有针对性
                    2. 语气积极鼓励，指出问题时给出可执行建议
                    3. 每条不超过 50 字
                    """)
                .user(prompt)
                .call()
                .content();

            log.info("AI 成长分析生成成功");
            return parseAiResponse(aiResponse);
        } catch (Exception e) {
            log.warn("AI 成长分析生成失败，返回默认分析", e);
            return defaultAnalysis();
        }
    }

    /**
     * 构造 AI 提示词，把聚合好的数据塞进去
     */
    private String buildPrompt(WeeklyReportVO vo) {
        StringBuilder sb = new StringBuilder();
        sb.append("以下是用户本期（").append(vo.getType().equals("month") ? "月报" : "周报").append("）学习数据：\n\n");

        sb.append("【学习时长】\n");
        sb.append("- 本期总时长：").append(formatSeconds(vo.getTotalStudySeconds())).append("\n");
        sb.append("- 日均时长：").append(formatSeconds(vo.getAvgDailySeconds())).append("\n");
        sb.append("- 上期总时长：").append(formatSeconds(vo.getPrevTotalStudySeconds())).append("\n");
        sb.append("- 环比变化：").append(vo.getChangePercent()).append("%\n\n");

        sb.append("【技能进度】\n");
        sb.append("- 已点亮技能：").append(vo.getMasteredCount()).append(" 个\n");
        sb.append("- 学习中技能：").append(vo.getStudyingCount()).append(" 个\n");
        sb.append("- 未开始技能：").append(vo.getNotStartedCount()).append(" 个\n");
        sb.append("- 本期新点亮：").append(vo.getNewMasteredCount()).append(" 个\n\n");

        sb.append("【学习活动】\n");
        sb.append("- AI 对话次数：").append(vo.getAiChatCount()).append("\n");
        sb.append("- AI 面试次数：").append(vo.getAiInterviewCount()).append("\n");
        sb.append("- 考试次数：").append(vo.getExamCount()).append("\n");
        sb.append("- 连续打卡天数：").append(vo.getConsecutiveDays()).append(" 天\n\n");

        if (!vo.getRecentRecords().isEmpty()) {
            sb.append("【近期学习记录】\n");
            for (LearningRecordVO r : vo.getRecentRecords()) {
                sb.append("- ").append(r.getSkillName())
                  .append("（").append(r.getSourceType()).append("）")
                  .append(" 进度：").append(r.getProgress()).append("%")
                  .append(" 时长：").append(formatSeconds(r.getStudySeconds() != null ? r.getStudySeconds() : 0))
                  .append("\n");
            }
        }

        sb.append("\n请基于以上数据生成结构化的成长分析报告。");
        return sb.toString();
    }

    /**
     * 解析 AI 返回的 JSON
     */
    private AiReportAnalysis parseAiResponse(String aiResponse) {
        try {
            // 兼容 AI 可能返回的 markdown 代码块包裹
            String json = aiResponse.trim();
            if (json.startsWith("```")) {
                json = json.replaceAll("^```(json)?\\s*", "").replaceAll("\\s*```$", "");
            }

            AiReportAnalysis analysis = objectMapper.readValue(json, AiReportAnalysis.class);
            // 确保列表字段非 null
            if (analysis.getStrengths() == null) analysis.setStrengths(new ArrayList<>());
            if (analysis.getWeaknesses() == null) analysis.setWeaknesses(new ArrayList<>());
            if (analysis.getSuggestions() == null) analysis.setSuggestions(new ArrayList<>());
            if (analysis.getNextPlan() == null) analysis.setNextPlan(new ArrayList<>());
            return analysis;
        } catch (Exception e) {
            log.warn("解析 AI 响应失败，使用默认分析: {}", e.getMessage());
            return defaultAnalysis();
        }
    }

    /**
     * 默认分析（AI 调用失败时的兜底）
     */
    private AiReportAnalysis defaultAnalysis() {
        AiReportAnalysis analysis = new AiReportAnalysis();
        analysis.setSummary("本期学习数据已为你整理完毕，继续保持学习热情！");
        analysis.getStrengths().add("坚持学习是最大的进步");
        analysis.getSuggestions().add("保持稳定的学习节奏");
        analysis.getSuggestions().add("多参与 AI 对话和面试练习");
        analysis.getNextPlan().add("继续推进正在学习的技能");
        return analysis;
    }

    /**
     * 秒数转可读时长
     */
    private String formatSeconds(int seconds) {
        if (seconds <= 0) return "0min";
        int minutes = seconds / 60;
        if (minutes < 60) return minutes + "min";
        return String.format("%.1fh", minutes / 60.0);
    }
}
