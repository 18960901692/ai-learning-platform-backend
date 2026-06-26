package com.aicompanion.model.vo;

import lombok.Data;

import java.util.List;

/**
 * 开始考核响应 VO（含会话ID和题目列表）
 */
@Data
public class StartExamVO {

    private Long sessionId;

    private List<ExamQuestionVO> questions;
}
