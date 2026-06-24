package com.aicompanion.service;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.model.dto.SkillDTO;
import com.aicompanion.model.vo.SkillVO;

import java.util.List;

/**
 * 技能服务接口
 */
public interface SkillService {

    /**
     * 新增技能
     */
    SkillVO createSkill(SkillDTO dto);

    /**
     * 更新技能
     */
    SkillVO updateSkill(Long id, SkillDTO dto);

    /**
     * 删除技能
     */
    void deleteSkill(Long id);

    /**
     * 根据 ID 获取技能
     */
    SkillVO getSkillById(Long id);

    /**
     * 分页查询技能列表
     */
    PageResult<SkillVO> getSkillList(int page, int pageSize, String category, String keyword);

    /**
     * 获取所有技能（不分页）
     */
    List<SkillVO> getAllSkills();

    /**
     * 获取所有分类
     */
    List<String> getAllCategories();

    /**
     * 获取技能树形结构
     */
    List<SkillVO> getSkillTree();
}
