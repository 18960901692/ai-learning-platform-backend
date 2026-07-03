package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 管理后台用户画像 VO
 */
@Data
public class AdminUserProfileVO {

    private Long userId;
    private String username;
    private String nickname;
    private String email;
    private String phone;
    private String role;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    // 学习统计
    private Integer totalStudySeconds;
    private Integer studyingSkillsCount;
    private Integer completedSkillsCount;
    private Integer consecutiveDays;

    // AI 统计
    private Integer aiChatCount;
    private Integer aiInterviewCount;
}
