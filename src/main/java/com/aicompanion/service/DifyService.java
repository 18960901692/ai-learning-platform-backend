package com.aicompanion.service;

import com.aicompanion.model.vo.DifyChatResponseVO;
import com.aicompanion.model.vo.DifyGradeResultVO;

/**
 * Dify AI 平台客户端服务
 */
public interface DifyService {

    /**
     * 调用 Dify /chat-messages（考核 App）
     * 用于多轮考核对话、出题、阅卷
     *
     * @param userId        当前用户 ID（Dify user 字段，用于隔离会话归属和统计）
     * @param skillName     技能名称（inputs.skill_name）
     * @param query         用户 query / 出题指令 / 阅卷指令
     * @param conversation  阅卷时的对话记录（inputs.conversation），其他场景传 null
     * @param conversationId Dify 会话 ID（多轮对话时传入），首轮传 null
     * @return Dify 原始响应（已剥离 answer 中的 <think> 标签）
     */
    DifyChatResponseVO chat(Long userId, String skillName, String query,
                            String conversation, String conversationId);

    /**
     * 生成考核题目
     *
     * @param userId    当前用户 ID
     * @param skillName 技能名称
     * @return Dify answer 原文（JSON 数组字符串）
     */
    String generateExamQuestions(Long userId, String skillName);

    /**
     * 阅卷评分（内部调 chat + ScoreExtractor）
     *
     * @param userId       当前用户 ID
     * @param skillName    技能名称
     * @param conversation 完整对话记录
     * @return 阅卷结果（含评阅文本和提取好的分数；score = -1 表示无法解析）
     */
    DifyGradeResultVO gradeExam(Long userId, String skillName, String conversation);

    /**
     * 调用 Dify /workflows/run（简历优化 Workflow）
     *
     * @param userId        当前用户 ID
     * @param resumeContent 简历内容
     * @return 优化后的简历文本；空输出或结构异常时抛业务异常
     */
    String optimizeResume(Long userId, String resumeContent);
}
