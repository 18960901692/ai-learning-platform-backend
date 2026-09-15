package com.aicompanion.common.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Dify 阅卷返回的文本中提取分数
 *
 * 鸿蒙端原版实现位于 ExamApi.ets:184-211，迁移到后端统一处理
 */
@Component
public class ScoreExtractor {

    /**
     * 默认分数（所有正则都不匹配时使用）
     */
    private static final int DEFAULT_SCORE = 50;

    /**
     * 格式 1: "score": 60 或 "score":60 或 "分数": 60
     */
    private static final Pattern SCORE_FIELD_PATTERN = Pattern.compile(
            "[\"']?score[\"']?\\s*[:：]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * 格式 2: 60 分 或 得分：60 或 60分
     */
    private static final Pattern SCORE_TEXT_PATTERN = Pattern.compile("(\\d+)\\s*分");

    /**
     * 格式 3: 总分 60/100 或 总分：60
     */
    private static final Pattern TOTAL_SCORE_PATTERN = Pattern.compile("总分\\s*[:：]?\\s*(\\d+)");

    /**
     * 从文本中提取分数
     *
     * @param text Dify 返回的评阅文本
     * @return 提取到的分数（0-100），未匹配返回默认值 50
     */
    public int extract(String text) {
        if (text == null || text.isBlank()) {
            return DEFAULT_SCORE;
        }

        // 格式 1: "score": 60  — 优先级最高（结构化）
        int score = tryMatch(text, SCORE_FIELD_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        // 格式 3: 总分 60
        score = tryMatch(text, TOTAL_SCORE_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        // 格式 2: 60 分
        score = tryMatch(text, SCORE_TEXT_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        return DEFAULT_SCORE;
    }

    private int tryMatch(String text, Pattern pattern) {
        Matcher matcher = pattern.matcher(text);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (NumberFormatException e) {
                return -1;
            }
        }
        return -1;
    }

    /**
     * 限制分数在 0-100 之间
     */
    private int clamp(int score) {
        return Math.max(0, Math.min(100, score));
    }
}
