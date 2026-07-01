package com.aicompanion.service;

import com.aicompanion.model.vo.ExamSessionVO;
import com.aicompanion.model.vo.StartExamVO;

import java.util.List;

/**
 * 考核服务接口
 * 注：题目生成和阅卷由 Dify 工作流完成，后端只负责会话管理和技能点亮
 */
public interface ExamService {

    /**
     * 开始考核
     * 1. 校验父技能状态
     * 2. 创建考核会话
     * 3. 返回会话ID（前端从 Dify 获取题目）
     */
    StartExamVO startExam(Long userId, Long skillId);

    /**
     * 获取考核结果
     */
    ExamSessionVO getExamResult(Long sessionId);

    /**
     * 保存 Dify 阅卷结果
     * 鸿蒙端调用 Dify 阅卷后，将评分结果传给后端保存
     */
    ExamSessionVO saveDifyGrade(Long userId, Long sessionId, String text, int score, List<Long> questionIds, List<String> userAnswers);
}