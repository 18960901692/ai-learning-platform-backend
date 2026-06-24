package com.aicompanion.model.vo;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 技能 VO
 */
@Data
public class SkillVO {

    private Long id;
    private String name;
    private String category;
    private String description;
    private Integer level;
    private Long parentId;
    private LocalDateTime createTime;

    /**
     * 子技能列表（树形结构用）
     */
    private List<SkillVO> children;
}
