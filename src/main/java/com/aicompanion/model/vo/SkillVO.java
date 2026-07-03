package com.aicompanion.model.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
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
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;

    /**
     * 子技能列表（树形结构用）
     */
    private List<SkillVO> children;

    /**
     * 用户对该技能的状态: 0=未开始, 1=学习中, 2=已点亮（从 learning_record 或 user_skill 获取）
     */
    private Integer userStatus = 0;

    /**
     * 用户对该技能的等级: 0=无, 1=入门, 2=基础, 3=熟练（从 user_skill 获取）
     */
    private Integer userLevel = 0;
}
