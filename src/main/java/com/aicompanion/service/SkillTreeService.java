package com.aicompanion.service;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.model.dto.SkillTreeDTO;
import com.aicompanion.model.vo.SkillTreeVO;

/**
 * 技能树服务接口
 */
public interface SkillTreeService {

    /**
     * 分页查询技能树列表
     */
    PageResult<SkillTreeVO> getSkillList(int page, int pageSize, String keyword, String category);

    /**
     * 新增技能
     */
    SkillTreeVO createSkill(SkillTreeDTO dto);

    /**
     * 修改技能
     */
    SkillTreeVO updateSkill(Long id, SkillTreeDTO dto);

    /**
     * 删除技能
     */
    void deleteSkill(Long id);
}
