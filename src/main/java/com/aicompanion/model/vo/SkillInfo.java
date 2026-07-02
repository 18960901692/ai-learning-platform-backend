package com.aicompanion.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 技能信息 VO（返回给 AI 的结构化数据）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SkillInfo {

    /**
     * 技能名称
     */
    private String name;

    /**
     * 掌握等级（0-5）
     */
    private int level;

    /**
     * 分类
     */
    private String category;

    /**
     * 状态：未学习 / 学习中 / 已掌握
     */
    private String status;
}
