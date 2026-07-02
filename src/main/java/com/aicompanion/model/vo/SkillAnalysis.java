package com.aicompanion.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 技能分析结果 VO（返回给 AI 的结构化数据）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillAnalysis {

    /**
     * 已掌握的技能列表
     */
    private List<String> mastered;

    /**
     * 学习中的技能列表
     */
    private List<String> learning;

    /**
     * 未学习的技能列表
     */
    private List<String> notStarted;
}
