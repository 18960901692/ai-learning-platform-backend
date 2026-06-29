package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.mapper.*;
import com.aicompanion.model.entity.*;
import com.aicompanion.model.vo.*;
import com.aicompanion.service.ExamService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

/**
 * 考核服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamQuestionMapper examQuestionMapper;
    private final ExamSessionMapper examSessionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ObjectMapper objectMapper;

    private static final int EXAM_QUESTION_COUNT = 5;
    private static final int PASS_THRESHOLD = 70; // 70%

    @Override
    @Transactional
    public StartExamVO startExam(Long userId, Long skillId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }

        // 校验父技能是否已掌握
        if (skill.getParentId() != null && skill.getParentId() > 0) {
            UserSkill parentSkill = userSkillMapper.selectOne(
                    new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                            .eq(UserSkill::getUserId, userId)
                            .eq(UserSkill::getSkillId, skill.getParentId())
                            .last("LIMIT 1")
            );
            if (parentSkill == null || parentSkill.getStatus() != 2) {
                Skill parent = skillMapper.selectById(skill.getParentId());
                String parentName = parent != null ? parent.getName() : "父技能";
                throw new BusinessException("请先掌握「" + parentName + "」后再来考核");
            }
        }

        // 从题库随机取题（不够则由 Dify 补充，目前先用题库）
        List<ExamQuestion> questions = examQuestionMapper.findRandomBySkillId(skillId, EXAM_QUESTION_COUNT);
        if (questions.size() < EXAM_QUESTION_COUNT) {
            log.warn("技能 {} 题库不足 {} 题，当前仅 {} 题", skillId, EXAM_QUESTION_COUNT, questions.size());
            // TODO: 调用 Dify 补全题目
        }

        // 创建考核会话
        ExamSession session = new ExamSession();
        session.setUserId(userId);
        session.setSkillId(skillId);
        session.setStatus("IN_PROGRESS");
        session.setTotalScore(0);
        session.setPassScore(PASS_THRESHOLD);
        session.setTotalQuestions(questions.size());
        session.setAnsweredCount(0);
        session.setStartTime(LocalDateTime.now());
        examSessionMapper.insert(session);

        // 创建作答占位记录
        for (int i = 0; i < questions.size(); i++) {
            ExamQuestion q = questions.get(i);
            ExamAnswer answer = new ExamAnswer();
            answer.setSessionId(session.getId());
            answer.setQuestionId(q.getId());
            answer.setQuestionOrder(i + 1);
            examAnswerMapper.insert(answer);
        }

        log.info("开始考核: userId={}, skillId={}, sessionId={}, totalQuestions={}",
                userId, skillId, session.getId(), questions.size());

        // 返回题目（不含答案）
        StartExamVO examVO = new StartExamVO();
        examVO.setSessionId(session.getId());
        examVO.setQuestions(questions.stream().map(q -> toQuestionVO(q)).collect(Collectors.toList()));
        return examVO;
    }

    @Override
    @Transactional
    public ExamSessionVO submitAndGrade(Long userId, Long sessionId,
                                         List<Long> questionIds, List<String> answers) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "考核会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此考核会话");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考核已结束，无法重复提交");
        }

        // 获取题目
        List<ExamAnswer> examAnswers = examAnswerMapper.findBySessionId(sessionId);
        Map<Long, ExamAnswer> answerMap = examAnswers.stream()
                .collect(Collectors.toMap(ExamAnswer::getQuestionId, a -> a));

        List<ExamQuestion> questions = examQuestionMapper.selectBatchIds(questionIds);
        Map<Long, ExamQuestion> questionMap = questions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        int totalScore = 0;
        int maxScore = 0;
        Skill skill = skillMapper.selectById(session.getSkillId());

        // 逐题评分
        for (int i = 0; i < questionIds.size(); i++) {
            Long qid = questionIds.get(i);
            String userAnswer = answers.get(i);

            ExamQuestion question = questionMap.get(qid);
            if (question == null) continue;

            ExamAnswer answerRecord = answerMap.get(qid);
            if (answerRecord == null) continue;

            int score = gradeQuestion(question, userAnswer);
            answerRecord.setUserAnswer(userAnswer);
            answerRecord.setScore(score);
            examAnswerMapper.updateById(answerRecord);

            totalScore += score;
            maxScore += (question.getScore() != null ? question.getScore() : 20);
        }

        // 计算百分比得分
        int percentage = maxScore > 0 ? (totalScore * 100 / maxScore) : 0;

        // 更新会话
        session.setTotalScore(percentage);
        session.setAnsweredCount(questionIds.size());
        session.setEndTime(LocalDateTime.now());

        if (percentage >= PASS_THRESHOLD) {
            session.setStatus("PASSED");
            // 点亮技能
            lightUpSkill(userId, session.getSkillId(), percentage);
        } else {
            session.setStatus("FAILED");
        }

        examSessionMapper.updateById(session);

        log.info("考核完成: sessionId={}, score={}%, status={}", sessionId, percentage, session.getStatus());

        return buildSessionVO(session, examAnswers, questions, skill);
    }

    @Override
    public ExamSessionVO getExamResult(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "考核会话不存在");
        }

        List<ExamAnswer> answers = examAnswerMapper.findBySessionId(sessionId);
        List<ExamQuestion> questions = examQuestionMapper.selectBatchIds(
                answers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toList()));
        Skill skill = skillMapper.selectById(session.getSkillId());

        return buildSessionVO(session, answers, questions, skill);
    }

    @Override
    @Transactional
    public ExamSessionVO saveDifyGrade(Long userId, Long sessionId, String text, int score, List<Long> questionIds, List<String> userAnswers) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "考核会话不存在");
        }
        if (!session.getUserId().equals(userId)) {
            throw new BusinessException("无权操作此考核会话");
        }
        if (!"IN_PROGRESS".equals(session.getStatus())) {
            throw new BusinessException("考核已结束，无法重复提交");
        }

        // 保存用户答案
        if (questionIds != null && userAnswers != null) {
            for (int i = 0; i < questionIds.size(); i++) {
                ExamAnswer answer = new ExamAnswer();
                answer.setSessionId(sessionId);
                answer.setQuestionId(questionIds.get(i));
                answer.setUserAnswer(userAnswers.get(i));
                examAnswerMapper.insert(answer);
            }
        }

        // 保存 Dify 评分结果
        session.setTotalScore(score);
        session.setAiFeedback(text);
        session.setAnsweredCount(session.getTotalQuestions());
        session.setEndTime(LocalDateTime.now());

        if (score >= PASS_THRESHOLD) {
            session.setStatus("PASSED");
            lightUpSkill(userId, session.getSkillId(), score);
        } else {
            session.setStatus("FAILED");
            // 考核失败也要更新 user_skill 为"学习中"状态
            updateSkillToLearning(userId, session.getSkillId());
        }

        examSessionMapper.updateById(session);

        log.info("Dify 阅卷完成: sessionId={}, score={}%, status={}", sessionId, score, session.getStatus());

        List<ExamAnswer> answers = examAnswerMapper.findBySessionId(sessionId);
        List<ExamQuestion> questions = examQuestionMapper.selectBatchIds(
                answers.stream().map(ExamAnswer::getQuestionId).collect(Collectors.toList()));
        Skill skill = skillMapper.selectById(session.getSkillId());

        return buildSessionVO(session, answers, questions, skill);
    }

    /**
     * 评分逻辑：选择题/判断题直接匹配答案，简答题关键词匹配
     */
    private int gradeQuestion(ExamQuestion question, String userAnswer) {
        if (userAnswer == null || userAnswer.trim().isEmpty()) {
            return 0;
        }

        String correctAnswer = question.getAnswer();
        if (correctAnswer == null) return 0;

        int score = question.getScore() != null ? question.getScore() : 20;

        switch (question.getType()) {
            case "SINGLE":
            case "MULTIPLE":
            case "JUDGE":
                return userAnswer.trim().equals(correctAnswer.trim()) ? score : 0;
            case "SHORT_ANSWER":
                // TODO: 调用 Dify 阅卷，先用关键词模糊匹配
                return gradeShortAnswer(userAnswer, correctAnswer, score);
            default:
                return userAnswer.trim().equalsIgnoreCase(correctAnswer.trim()) ? score : 0;
        }
    }

    /**
     * 简答题简单评分：检查关键词覆盖率
     */
    private int gradeShortAnswer(String userAnswer, String correctAnswer, int maxScore) {
        String[] keywords = correctAnswer.split("[，,。.;；、\\s]+");
        int matchCount = 0;
        for (String keyword : keywords) {
            if (keyword.length() >= 2 && userAnswer.contains(keyword)) {
                matchCount++;
            }
        }
        if (keywords.length == 0) return 0;
        int rate = matchCount * 100 / keywords.length;
        if (rate >= 80) return maxScore;
        if (rate >= 50) return maxScore * 2 / 3;
        if (rate >= 30) return maxScore / 3;
        return 0;
    }

    /**
     * 点亮技能
     */
    private void lightUpSkill(Long userId, Long skillId, int score) {
        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        int level = score >= 90 ? 3 : (score >= 80 ? 2 : 1);

        if (userSkill == null) {
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setLevel(level);
            userSkill.setStatus(2);
            userSkillMapper.insert(userSkill);
        } else {
            userSkill.setLevel(Math.max(userSkill.getLevel(), level));
            userSkill.setStatus(2);
            userSkillMapper.updateById(userSkill);
        }

        // 更新学习记录
        LearningRecord record = learningRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getSkillId, skillId)
                        .eq(LearningRecord::getStatus, 1)
                        .last("LIMIT 1")
        );
        if (record != null) {
            record.setStatus(2);
            record.setProgress(100);
            record.setCompleteTime(LocalDateTime.now());
            // 确保学习时长不为null
            if (record.getStudySeconds() == null) {
                record.setStudySeconds(0);
            }
            learningRecordMapper.updateById(record);
        }

        log.info("点亮技能: userId={}, skillId={}, level={}", userId, skillId, level);
    }

    /**
     * 更新技能为"学习中"状态（考核失败时调用）
     */
    private void updateSkillToLearning(Long userId, Long skillId) {
        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        if (userSkill == null) {
            // 如果还没有记录，创建一条"学习中"的记录
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setLevel(0);
            userSkill.setStatus(1); // 学习中
            userSkillMapper.insert(userSkill);
        } else if (userSkill.getStatus() == 0) {
            // 如果状态是"未开始"，更新为"学习中"
            userSkill.setStatus(1);
            userSkillMapper.updateById(userSkill);
        }
        // 如果已经是"已点亮"(status=2)，不降级

        log.info("更新技能为学习中: userId={}, skillId={}", userId, skillId);
    }

    /**
     * 构建考核结果 VO
     */
    private ExamSessionVO buildSessionVO(ExamSession session, List<ExamAnswer> answers,
                                          List<ExamQuestion> questions, Skill skill) {
        ExamSessionVO vo = new ExamSessionVO();
        vo.setSessionId(session.getId());
        vo.setSkillId(session.getSkillId());
        vo.setSkillName(skill != null ? skill.getName() : "");
        vo.setStatus(session.getStatus());
        vo.setTotalScore(session.getTotalScore());
        vo.setPassScore(session.getPassScore());
        vo.setPassed("PASSED".equals(session.getStatus()));
        vo.setTotalQuestions(session.getTotalQuestions());
        vo.setAnsweredCount(session.getAnsweredCount());
        vo.setAiFeedback(session.getAiFeedback());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());

        Map<Long, ExamQuestion> qMap = questions.stream()
                .collect(Collectors.toMap(ExamQuestion::getId, q -> q));

        List<ExamAnswerVO> answerVOs = answers.stream().map(a -> {
            ExamAnswerVO avo = new ExamAnswerVO();
            avo.setQuestionId(a.getQuestionId());
            ExamQuestion q = qMap.get(a.getQuestionId());
            avo.setQuestion(q != null ? q.getQuestion() : "");
            avo.setQuestionOrder(a.getQuestionOrder());
            avo.setUserAnswer(a.getUserAnswer());
            avo.setScore(a.getScore());
            avo.setAiComment(a.getAiComment());
            return avo;
        }).collect(Collectors.toList());

        vo.setAnswers(answerVOs);
        return vo;
    }

    /**
     * 题目转为 VO（不含答案）
     */
    private ExamQuestionVO toQuestionVO(ExamQuestion q) {
        ExamQuestionVO vo = new ExamQuestionVO();
        vo.setId(q.getId());
        vo.setType(q.getType());
        vo.setQuestion(q.getQuestion());
        vo.setScore(q.getScore());
        vo.setDifficulty(q.getDifficulty());
        // 解析 options JSON 字符串为 List<String>
        if (q.getOptions() != null && !q.getOptions().isEmpty()) {
            try {
                List<String> options = objectMapper.readValue(q.getOptions(), new TypeReference<List<String>>() {});
                vo.setOptions(options);
            } catch (JsonProcessingException e) {
                log.warn("解析 options JSON 失败: {}", q.getOptions(), e);
                vo.setOptions(Collections.emptyList());
            }
        } else {
            vo.setOptions(Collections.emptyList());
        }
        return vo;
    }
}
