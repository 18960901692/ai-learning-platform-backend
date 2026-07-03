package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台学习记录 VO
 */
@Data
public class AdminLearningRecordVO {

    private Long id;
    private Long userId;
    private String username;
    private String nickname;
    private Long skillId;
    private String skillName;
    private Integer progress;
    private Integer studySeconds;
    private Integer status;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastStudyTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
