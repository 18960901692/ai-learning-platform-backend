package com.aicompanion.common.util;

import org.springframework.stereotype.Component;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * 从 Dify 阅卷返回的文本中提取分数
 *
 * 鸿蒙端原版实现位于 ExamApi.ets:184-211，迁移到后端统一处理。
 * 与原版的关键差异：全部正则不匹配时返回 -1（表示"无法解析"），
 * 而不是给一个模糊的默认 50 分，避免 AI 拒答或格式变动时静默给及格。
 */
@Component
public class ScoreExtractor {

    /**
     * 格式 1: "score": 60 或 "score":60（最可靠，Dify prompt 应优先输出这种）
     */
    private static final Pattern SCORE_FIELD_PATTERN = Pattern.compile(
            "[\"']?score[\"']?\\s*[:：]\\s*(\\d+)", Pattern.CASE_INSENSITIVE);

    /**
     * 格式 2: 总分 60 或 总分：60（次可靠）
     */
    private static final Pattern TOTAL_SCORE_PATTERN = Pattern.compile("总分\\s*[:：]?\\s*(\\d+)");

    /**
     * 格式 3: 60 分 或 得分：60（最脆弱，最后才匹配；
     *   容易误匹配"每题20分"这种，所以前面必须已经命中 score/total_score）
     */
    private static final Pattern SCORE_TEXT_PATTERN = Pattern.compile(
            "(?<!\\d)(?!\\d+\\s*[分]\\s*[,，]\\s*共)(\\d+)\\s*分");

    /**
     * 从文本中提取分数
     *
     * @param text Dify 返回的评阅文本
     * @return 0-100 的分数，-1 表示"无法解析"
     */
    public int extract(String text) {
        if (text == null || text.isBlank()) {
            return -1;
        }

        // 1. "score": 60 — 优先，结构化最可靠
        int score = tryMatch(text, SCORE_FIELD_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        // 2. 总分 60
        score = tryMatch(text, TOTAL_SCORE_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        // 3. 60 分 — 兜底，易误匹配
        score = tryMatch(text, SCORE_TEXT_PATTERN);
        if (score >= 0) {
            return clamp(score);
        }

        // 全部不匹配 → 返回 -1，由上层决定如何处理（抛异常/让客户端知道阅卷失败）
        return -1;
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
