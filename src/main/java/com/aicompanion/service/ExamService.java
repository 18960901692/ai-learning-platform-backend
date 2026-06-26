package com.aicompanion.service;

import com.aicompanion.model.vo.ExamSessionVO;
import com.aicompanion.model.vo.StartExamVO;

import java.util.List;

/**
 * 考核服务接口
 */
public interface ExamService {

    /**
     * 开始考核
     * 1. 校验父技能状态
     * 2. 从题库/AI 获取 5 道题
     * 3. 创建考核会话
     * 4. 返回会话ID和题目（不含答案）
     */
    StartExamVO startExam(Long userId, Long skillId);

    /**
     * 提交答案并评分
     * 用户答完所有题后一次性提交，调用 Dify 阅卷
     * 计算总分，判断 PASSED/FAILED
     * 通过则点亮技能
     */
    ExamSessionVO submitAndGrade(Long userId, Long sessionId, List<Long> questionIds, List<String> answers);

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
