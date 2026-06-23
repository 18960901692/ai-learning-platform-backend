package com.aicompanion.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 技能树 VO
 */
@Data
public class SkillTreeVO {

    private Long id;
    private String name;
    private String category;
    private String description;
    private Integer level;
    private Long parentId;
    private LocalDateTime createTime;
}
