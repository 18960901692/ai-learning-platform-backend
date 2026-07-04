package com.aicompanion.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * AI 成长分析结果
 */
@Data
public class AiReportAnalysis {

    /** 总体评价 */
    private String summary;

    /** 优势列表 */
    private List<String> strengths = new ArrayList<>();

    /** 不足列表 */
    private List<String> weaknesses = new ArrayList<>();

    /** 改进建议 */
    private List<String> suggestions = new ArrayList<>();

    /** 下周计划 */
    private List<String> nextPlan = new ArrayList<>();
}
