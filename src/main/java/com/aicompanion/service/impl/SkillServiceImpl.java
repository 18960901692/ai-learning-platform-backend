package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.dto.SkillDTO;
import com.aicompanion.model.entity.LearningRecord;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.SkillVO;
import com.aicompanion.service.SkillService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 技能服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SkillServiceImpl implements SkillService {

    private final SkillMapper skillMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final UserSkillMapper userSkillMapper;

    @Override
    public SkillVO createSkill(SkillDTO dto) {
        Skill skill = new Skill();
        skill.setName(dto.getName());
        skill.setCategory(dto.getCategory());
        skill.setDescription(dto.getDescription());
        skill.setLevel(dto.getLevel() != null ? dto.getLevel() : 1);
        skill.setParentId(dto.getParentId() != null ? dto.getParentId() : 0L);

        skillMapper.insert(skill);
        log.info("新增技能成功: {}", dto.getName());

        return toSkillVO(skill);
    }

    @Override
    public SkillVO updateSkill(Long id, SkillDTO dto) {
        Skill skill = skillMapper.selectById(id);
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

        skillMapper.updateById(skill);
        log.info("更新技能成功: {}", dto.getName());

        return toSkillVO(skill);
    }

    @Override
    public void deleteSkill(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }

        skillMapper.deleteById(id);
        log.info("删除技能成功: {}", skill.getName());
    }

    @Override
    public SkillVO getSkillById(Long id) {
        Skill skill = skillMapper.selectById(id);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }
        return toSkillVO(skill);
    }

    @Override
    public PageResult<SkillVO> getSkillList(int page, int pageSize, String category, String keyword) {
        Page<Skill> pageParam = new Page<>(page, pageSize);
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();

        if (StringUtils.hasText(category)) {
            wrapper.eq(Skill::getCategory, category);
        }

        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w
                    .like(Skill::getName, keyword)
                    .or()
                    .like(Skill::getDescription, keyword)
            );
        }

        wrapper.orderByDesc(Skill::getCreateTime);

        Page<Skill> result = skillMapper.selectPage(pageParam, wrapper);

        List<SkillVO> voList = result.getRecords().stream()
                .map(this::toSkillVO)
                .toList();

        return PageResult.of(result.getTotal(), voList, result.getCurrent(), result.getSize());
    }

    @Override
    public List<SkillVO> getAllSkills() {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Skill::getLevel).orderByAsc(Skill::getCreateTime);

        return skillMapper.selectList(wrapper).stream()
                .map(this::toSkillVO)
                .toList();
    }

    @Override
    public List<String> getAllCategories() {
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.select(Skill::getCategory);

        return skillMapper.selectList(wrapper).stream()
                .map(Skill::getCategory)
                .distinct()
                .toList();
    }

    @Override
    public List<SkillVO> getSkillTree(Long userId) {
        // 获取所有技能
        LambdaQueryWrapper<Skill> wrapper = new LambdaQueryWrapper<>();
        wrapper.orderByAsc(Skill::getLevel).orderByAsc(Skill::getCreateTime);
        List<SkillVO> allSkills = skillMapper.selectList(wrapper).stream()
                .map(this::toSkillVO)
                .collect(Collectors.toList());

        // 获取用户的学习记录（按 skillId 分组）
        LambdaQueryWrapper<LearningRecord> lrWrapper = new LambdaQueryWrapper<>();
        lrWrapper.eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getDeleted, 0);
        List<LearningRecord> learningRecords = learningRecordMapper.selectList(lrWrapper);
        
        // 建立 skillId -> learningRecord 的映射
        Map<Long, LearningRecord> learningRecordMap = new HashMap<>();
        for (LearningRecord record : learningRecords) {
            // 如果有多个记录，取最新的一个
            LearningRecord existing = learningRecordMap.get(record.getSkillId());
            if (existing == null || record.getCreateTime().isAfter(existing.getCreateTime())) {
                learningRecordMap.put(record.getSkillId(), record);
            }
        }

        // 获取用户的技能掌握情况
        LambdaQueryWrapper<UserSkill> usWrapper = new LambdaQueryWrapper<>();
        usWrapper.eq(UserSkill::getUserId, userId);
        List<UserSkill> userSkills = userSkillMapper.selectList(usWrapper);
        log.info("getSkillTree: userId={}, 查询到 user_skill 记录数={}", userId, userSkills.size());
        for (UserSkill us : userSkills) {
            log.info("  user_skill: skillId={}, status={}, level={}", us.getSkillId(), us.getStatus(), us.getLevel());
        }
        
        // 建立 skillId -> userSkill 的映射
        Map<Long, UserSkill> userSkillMap = new HashMap<>();
        for (UserSkill us : userSkills) {
            userSkillMap.put(us.getSkillId(), us);
        }

        // 合并学习状态到技能 VO
        for (SkillVO skill : allSkills) {
            // 优先级: user_skill > learning_record
            UserSkill us = userSkillMap.get(skill.getId());
            if (us != null) {
                skill.setUserStatus(us.getStatus());
                skill.setUserLevel(us.getLevel());
                log.debug("技能[{}]状态: userSkill status={}, level={}", skill.getName(), us.getStatus(), us.getLevel());
            } else {
                // 从学习记录获取状态（仅当 user_skill 不存在时）
                LearningRecord record = learningRecordMap.get(skill.getId());
                if (record != null) {
                    skill.setUserStatus(record.getStatus());
                    skill.setUserLevel(0);
                    log.debug("技能[{}]状态: learningRecord status={}", skill.getName(), record.getStatus());
                } else {
                    log.debug("技能[{}]状态: 无记录，默认 status=0, level=0", skill.getName());
                }
            }
        }

        // 构建树形结构
        List<SkillVO> tree = new ArrayList<>();
        for (SkillVO skill : allSkills) {
            if (skill.getParentId() == null || skill.getParentId() == 0) {
                // 顶级节点
                tree.add(skill);
            } else {
                // 子节点，找到父节点并添加
                for (SkillVO parent : allSkills) {
                    if (parent.getId().equals(skill.getParentId())) {
                        if (parent.getChildren() == null) {
                            parent.setChildren(new ArrayList<>());
                        }
                        parent.getChildren().add(skill);
                        break;
                    }
                }
            }
        }
        return tree;
    }

    private SkillVO toSkillVO(Skill skill) {
        SkillVO vo = new SkillVO();
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
