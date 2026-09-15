package com.aicompanion.tool;

import com.aicompanion.common.util.UserContextHolder;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.SkillAnalysis;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 用户技能分析工具 - 分析当前登录用户的技能掌握情况
 *
 * <p>无状态 Bean：userId 从 UserContextHolder（ThreadLocal）取，不存字段。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class UserSkillAnalysisTool {

    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;

    @Tool(description = "分析当前登录用户的技能掌握情况。返回已掌握的技能列表、学习中的技能列表、未学习的技能列表。")
    public SkillAnalysis analyzeUserSkills() {
        Long userId = UserContextHolder.get();
        log.info("UserSkillAnalysisTool 被调用: userId={}", userId);

        if (userId == null) {
            return new SkillAnalysis(List.of(), List.of(), List.of("用户未登录"));
        }

        // 查询所有技能
        List<Skill> allSkills = skillMapper.selectList(
                new LambdaQueryWrapper<Skill>().eq(Skill::getDeleted, 0)
        );

        // 查询用户已有的技能记录
        List<UserSkill> userSkills = userSkillMapper.selectList(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
        );
        Map<Long, UserSkill> skillMap = userSkills.stream()
                .collect(Collectors.toMap(UserSkill::getSkillId, us -> us));

        // 分类
        List<String> mastered = new ArrayList<>();
        List<String> learning = new ArrayList<>();
        List<String> notStarted = new ArrayList<>();

        for (Skill skill : allSkills) {
            UserSkill userSkill = skillMap.get(skill.getId());
            if (userSkill == null) {
                notStarted.add(skill.getName());
            } else {
                int status = userSkill.getStatus();
                int level = userSkill.getLevel();
                String label = skill.getName() + "(等级" + level + ")";
                if (status == 2) {
                    mastered.add(label);
                } else if (status == 1) {
                    learning.add(label);
                } else {
                    notStarted.add(label);
                }
            }
        }

        return new SkillAnalysis(mastered, learning, notStarted);
    }
}
