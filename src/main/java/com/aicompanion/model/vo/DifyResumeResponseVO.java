package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Dify /workflows/run API 的 blocking 模式响应
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class DifyResumeResponseVO {

    @JsonProperty("workflow_run_id")
    private String workflowRunId;

    private WorkflowData data;

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class WorkflowData {
        private ResumeOutputs outputs;
    }

    @Data
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ResumeOutputs {
        /**
         * Dify 简历优化工作流输出字段
         */
        @JsonProperty("optimized_resume")
        private String optimizedResume;
    }
}
