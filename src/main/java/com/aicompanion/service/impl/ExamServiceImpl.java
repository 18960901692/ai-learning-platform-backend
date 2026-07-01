package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.mapper.*;
import com.aicompanion.model.entity.*;
import com.aicompanion.model.vo.*;
import com.aicompanion.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 考核服务实现
 * 注：题目生成和阅卷由 Dify 工作流完成，后端只负责会话管理和技能点亮
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ExamServiceImpl implements ExamService {

    private final ExamSessionMapper examSessionMapper;
    private final ExamAnswerMapper examAnswerMapper;
    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;
    private final LearningRecordMapper learningRecordMapper;

    private static final int PASS_THRESHOLD = 60; // 60%

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

        // 创建考核会话（题目由 Dify 动态生成，不从本地题库取题）
        ExamSession session = new ExamSession();
        session.setUserId(userId);
        session.setSkillId(skillId);
        session.setStatus("IN_PROGRESS");
        session.setTotalScore(0);
        session.setPassScore(PASS_THRESHOLD);
        session.setTotalQuestions(5); // Dify 默认出5题
        session.setAnsweredCount(0);
        session.setStartTime(LocalDateTime.now());
        examSessionMapper.insert(session);

        log.info("开始考核: userId={}, skillId={}, sessionId={}", userId, skillId, session.getId());

        // 返回会话ID（前端从 Dify 获取题目）
        StartExamVO examVO = new StartExamVO();
        examVO.setSessionId(session.getId());
        return examVO;
    }

    @Override
    @Transactional
    public ExamSessionVO saveDifyGrade(Long userId, Long sessionId, String text, int score, List<Long> questionIds, List<String> userAnswers) {
        log.info("开始保存Dify阅卷结果: userId={}, sessionId={}, score={}, text长度={}", userId, sessionId, score, text != null ? text.length() : 0);
        
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
        if (questionIds != null && userAnswers != null && questionIds.size() == userAnswers.size()) {
            for (int i = 0; i < questionIds.size(); i++) {
                ExamAnswer answer = new ExamAnswer();
                answer.setSessionId(sessionId);
                answer.setQuestionId(questionIds.get(i));
                answer.setQuestionOrder(i + 1);
                answer.setUserAnswer(userAnswers.get(i));
                examAnswerMapper.insert(answer);
            }
        }

        // 保存 Dify 评分结果
        session.setTotalScore(score);
        session.setAiFeedback(text);
        session.setAnsweredCount(session.getTotalQuestions());
        session.setEndTime(LocalDateTime.now());

        log.info("准备更新exam_session: sessionId={}, score={}, aiFeedback长度={}, status将变为={}", 
                sessionId, score, text != null ? text.length() : 0, score >= PASS_THRESHOLD ? "PASSED" : "FAILED");

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

        // 返回考核结果
        return getExamResult(sessionId);
    }

    @Override
    public ExamSessionVO getExamResult(Long sessionId) {
        ExamSession session = examSessionMapper.selectById(sessionId);
        if (session == null) {
            throw new BusinessException(404, "考核会话不存在");
        }

        List<ExamAnswer> answers = examAnswerMapper.findBySessionId(sessionId);
        Skill skill = skillMapper.selectById(session.getSkillId());

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

        // 构建答案列表
        List<ExamAnswerVO> answerVOs = answers.stream().map(a -> {
            ExamAnswerVO avo = new ExamAnswerVO();
            avo.setQuestionId(a.getQuestionId());
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
     * 点亮技能
     */
    private void lightUpSkill(Long userId, Long skillId, int score) {
        log.info("开始点亮技能: userId={}, skillId={}, score={}", userId, skillId, score);
        
        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        int level = score >= 90 ? 3 : (score >= 80 ? 2 : 1);

        if (userSkill == null) {
            log.info("创建新的user_skill记录: userId={}, skillId={}, level={}, status=2", userId, skillId, level);
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setLevel(level);
            userSkill.setStatus(2);
            userSkillMapper.insert(userSkill);
        } else {
            log.info("更新现有user_skill记录: userId={}, skillId={}, 原status={}, 新status=2", userId, skillId, userSkill.getStatus());
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
            log.info("更新learning_record: recordId={}, 原status={}, 新status=2", record.getId(), record.getStatus());
            record.setStatus(2);
            record.setProgress(100);
            record.setCompleteTime(LocalDateTime.now());
            // 确保学习时长不为null
            if (record.getStudySeconds() == null) {
                record.setStudySeconds(0);
            }
            learningRecordMapper.updateById(record);
        } else {
            log.warn("未找到status=1的learning_record: userId={}, skillId={}", userId, skillId);
        }

        log.info("点亮技能完成: userId={}, skillId={}, level={}", userId, skillId, level);
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
}