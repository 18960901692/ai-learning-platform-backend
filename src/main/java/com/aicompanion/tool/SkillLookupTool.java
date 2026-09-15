package com.aicompanion.tool;

import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.SkillInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.ai.tool.annotation.ToolParam;
import org.springframework.stereotype.Component;

/**
 * 技能查询工具 - 查询当前登录用户对指定技能的掌握情况
 *
 * <p>无状态 Bean：userId 通过 Spring AI ToolContext 参数注入，
 * 由调用方 chatClient.prompt().toolContext(Map.of("userId", userId)) 传入，
 * 同步/流式路径都生效（数据级传递，不依赖 ThreadLocal）。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SkillLookupTool {

    private final UserSkillMapper userSkillMapper;
    private final SkillMapper skillMapper;

    @Tool(description = "查询当前登录用户对指定技能的掌握情况。返回技能名称、掌握等级、分类、学习状态。")
    public SkillInfo lookupSkill(
            @ToolParam(description = "技能名称，如'Java基础'、'MySQL'、'Vue3'") String skillName,
            ToolContext toolContext
    ) {
        Long userId = (Long) toolContext.getContext().get("userId");
        log.info("SkillLookupTool 被调用: userId={}, skillName={}", userId, skillName);

        if (userId == null) {
            return new SkillInfo(skillName, 0, "未找到", "用户未登录");
        }

        // 查技能表（模糊匹配）
        Skill skill = skillMapper.selectOne(
                new LambdaQueryWrapper<Skill>()
                        .like(Skill::getName, skillName)
                        .last("LIMIT 1")
        );
        if (skill == null) {
            return new SkillInfo(skillName, 0, "未找到", "未学习");
        }

        // 查用户技能关联表
        UserSkill userSkill = userSkillMapper.selectOne(
                new LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skill.getId())
        );

        int level = userSkill != null ? userSkill.getLevel() : 0;
        int status = userSkill != null ? userSkill.getStatus() : 0;

        String statusText = switch (status) {
            case 2 -> "已掌握";
            case 1 -> "学习中";
            default -> "未学习";
        };

        return new SkillInfo(skill.getName(), level, skill.getCategory(), statusText);
    }
}
