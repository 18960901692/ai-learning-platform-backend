package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.response.PageResult;
import com.aicompanion.mapper.SkillTreeMapper;
import com.aicompanion.model.dto.SkillTreeDTO;
import com.aicompanion.model.entity.SkillTree;
import com.aicompanion.model.vo.SkillTreeVO;
import com.aicompanion.service.SkillTreeService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 技能树服务实现类
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillTreeServiceImpl implements SkillTreeService {

    private final SkillTreeMapper skillTreeMapper;

    @Override
    public PageResult<SkillTreeVO> getSkillList(int page, int pageSize, String keyword, String category) {
        Page<SkillTree> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<SkillTree> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(keyword)) {
            wrapper.like(SkillTree::getName, keyword);
        }

        if (StringUtils.hasText(category)) {
            wrapper.eq(SkillTree::getCategory, category);
        }

        wrapper.orderByDesc(SkillTree::getCreateTime);

        Page<SkillTree> result = skillTreeMapper.selectPage(pageParam, wrapper);

        List<SkillTreeVO> voList = result.getRecords().stream()
                .map(this::toVO)
                .toList();

        return PageResult.of(result.getTotal(), voList, result.getCurrent(), result.getSize());
    }

    @Override
    public SkillTreeVO createSkill(SkillTreeDTO dto) {
        SkillTree skill = new SkillTree();
        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        skill.setDescription(dto.getDescription());
        skill.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        skill.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);

        skillTreeMapper.insert(skill);
        log.info("新增技能成功: {}", dto.getName());

        return toVO(skill);
    }

    @Override
    public SkillTreeVO updateSkill(Long id, SkillTreeDTO dto) {
        SkillTree skill = skillTreeMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }

        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        skill.setDescription(dto.getDescription());
        if (dto.getLevel() != null) {
            skill.setLevel(dto.getLevel());
        }
        if (dto.getParentId() != null) {
            skill.setParentId(dto.getParentId());
        }

        skillTreeMapper.updateById(skill);
        log.info("修改技能成功: {}", dto.getName());

        return toVO(skill);
    }

    @Override
    public void deleteSkill(Long id) {
        SkillTree skill = skillTreeMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }

        skillTreeMapper.deleteById(id);
        log.info("删除技能成功: {}", skill.getName());
    }

    private SkillTreeVO toVO(SkillTree skill) {
        SkillTreeVO vo = new SkillTreeVO();
        vo.setId(skill.getId());
        vo.setName(skill.getName());
        vo.setCategory(skill.getCategory());
        vo.setDescription(skill.getDescription());
        vo.setLevel(skill.getLevel());
        vo.setParentId(skill.getParentId());
        vo.setCreateTime(skill.getCreateTime());
        return vo;
    }
}
