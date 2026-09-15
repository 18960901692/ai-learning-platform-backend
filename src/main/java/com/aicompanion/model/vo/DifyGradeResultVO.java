package com.aicompanion.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 阅卷评分结果（后端已从 Dify 文本中提取好分数）
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DifyGradeResultVO {

    /**
     * Dify 返回的完整评阅文本（可能包含评分理由）
     */
    private String text;

    /**
     * 从文本中解析出的分数（0-100）
     */
    private int score;
}
