package com.aicompanion.service.impl;

import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.entity.LearningRecord;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.LearningPathVO;
import com.aicompanion.service.LearningPathService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 学习路径推荐服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningPathServiceImpl implements LearningPathService {

    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;
    private final LearningRecordMapper learningRecordMapper;
    private final ChatClient chatClient;

    @Override
    public List<LearningPathVO> getRecommendations(Long userId) {
        log.info("开始生成用户[{}]的学习路径推荐", userId);

        // 1. 获取用户已掌握的技能（status=2）
        List<UserSkill> masteredSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getStatus, 2)
        );
        Set<Long> masteredSkillIds = masteredSkills.stream()
            .map(UserSkill::getSkillId)
            .collect(Collectors.toSet());

        // 2. 获取用户学习中的技能（status=1）
        List<LearningRecord> learningRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getStatus, 1)
                .eq(LearningRecord::getDeleted, 0)
        );
        Set<Long> learningSkillIds = learningRecords.stream()
            .map(LearningRecord::getSkillId)
            .collect(Collectors.toSet());

        // 3. 获取所有技能
        List<Skill> allSkills = skillMapper.selectList(
            new LambdaQueryWrapper<Skill>()
                .eq(Skill::getDeleted, 0)
        );
        Map<Long, Skill> skillMap = allSkills.stream()
            .collect(Collectors.toMap(Skill::getId, s -> s));

        // 4. 应用推荐规则
        Map<Long, LearningPathVO> recommendations = new LinkedHashMap<>();

        // 规则1：已掌握技能的直接子技能（优先级10）
        for (Long masteredId : masteredSkillIds) {
            Skill parentSkill = skillMap.get(masteredId);
            if (parentSkill == null) continue;

            for (Skill skill : allSkills) {
                if (skill.getParentId() != null && skill.getParentId().equals(masteredId)
                    && !masteredSkillIds.contains(skill.getId())
                    && !learningSkillIds.contains(skill.getId())) {
                    recommendations.put(skill.getId(), createRecommendation(skill, 10,
                        "基于你已掌握的「" + parentSkill.getName() + "」技能"));
                }
            }
        }

        // 规则2：同分类下，难度等级比已掌握最高技能高1级的技能（优先级8）
        Map<String, Integer> categoryMaxLevel = new HashMap<>();
        for (Long masteredId : masteredSkillIds) {
            Skill skill = skillMap.get(masteredId);
            if (skill != null) {
                categoryMaxLevel.merge(skill.getCategory(), skill.getLevel(), Math::max);
            }
        }

        for (Map.Entry<String, Integer> entry : categoryMaxLevel.entrySet()) {
            String category = entry.getKey();
            int maxLevel = entry.getValue();
            int nextLevel = maxLevel + 1;

            for (Skill skill : allSkills) {
                if (skill.getCategory().equals(category)
                    && skill.getLevel() == nextLevel
                    && !masteredSkillIds.contains(skill.getId())
                    && !learningSkillIds.contains(skill.getId())) {
                    recommendations.put(skill.getId(), createRecommendation(skill, 8,
                        "在「" + category + "」领域继续进阶"));
                }
            }
        }

        // 规则3：用户学习中技能的关联技能（同一父技能下的兄弟技能）（优先级6）
        for (Long learningId : learningSkillIds) {
            Skill learningSkill = skillMap.get(learningId);
            if (learningSkill == null || learningSkill.getParentId() == null
                || learningSkill.getParentId() == 0) continue;

            Long parentId = learningSkill.getParentId();
            for (Skill skill : allSkills) {
                if (skill.getParentId() != null && skill.getParentId().equals(parentId)
                    && !skill.getId().equals(learningId)
                    && !masteredSkillIds.contains(skill.getId())
                    && !learningSkillIds.contains(skill.getId())) {
                    recommendations.put(skill.getId(), createRecommendation(skill, 6,
                        "与正在学习的「" + learningSkill.getName() + "」相关联"));
                }
            }
        }

        // 规则4：热门技能（兜底策略，优先级4）
        // 统计所有用户学习次数最多的技能
        List<LearningRecord> allRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getDeleted, 0)
        );
        Map<Long, Long> skillLearnCount = allRecords.stream()
            .collect(Collectors.groupingBy(LearningRecord::getSkillId, Collectors.counting()));

        List<Skill> hotSkills = skillLearnCount.entrySet().stream()
            .sorted(Map.Entry.<Long, Long>comparingByValue().reversed())
            .limit(5)
            .map(e -> skillMap.get(e.getKey()))
            .filter(s -> s != null && !masteredSkillIds.contains(s.getId())
                && !learningSkillIds.contains(s.getId())
                && !recommendations.containsKey(s.getId()))
            .collect(Collectors.toList());

        for (Skill skill : hotSkills) {
            recommendations.put(skill.getId(), createRecommendation(skill, 4,
                "热门技能，很多学员正在学习"));
        }

        // 5. 过滤掉父技能未点亮的推荐（父技能未解锁则子技能不可推荐）
        List<LearningPathVO> eligible = recommendations.values().stream()
            .filter(vo -> {
                Skill skill = skillMap.get(vo.getSkillId());
                if (skill == null) return false;
                // 根技能（无父技能）直接放行
                if (skill.getParentId() == null || skill.getParentId() == 0) return true;
                // 有父技能的，父技能必须在已掌握集合中
                return masteredSkillIds.contains(skill.getParentId());
            })
            .collect(Collectors.toList());

        // 6. 按优先级排序并返回
        List<LearningPathVO> result = eligible.stream()
            .sorted((a, b) -> b.getPriority() - a.getPriority())
            .limit(6)
            .collect(Collectors.toList());

        log.info("用户[{}]的学习路径推荐完成，共{}个推荐", userId, result.size());
        return result;
    }

    @Override
    public List<LearningPathVO> getRecommendationsWithAi(Long userId) {
        // 1. 先用规则引擎生成推荐列表
        List<LearningPathVO> recommendations = getRecommendations(userId);

        if (recommendations.isEmpty()) {
            return recommendations;
        }

        // 2. 获取用户学习画像
        List<UserSkill> masteredSkills = userSkillMapper.selectList(
            new LambdaQueryWrapper<UserSkill>()
                .eq(UserSkill::getUserId, userId)
                .eq(UserSkill::getStatus, 2)
        );
        List<LearningRecord> learningRecords = learningRecordMapper.selectList(
            new LambdaQueryWrapper<LearningRecord>()
                .eq(LearningRecord::getUserId, userId)
                .eq(LearningRecord::getStatus, 1)
                .eq(LearningRecord::getDeleted, 0)
        );

        // 3. 构建AI Prompt
        String prompt = buildAiPrompt(userId, masteredSkills, learningRecords, recommendations);

        try {
            // 4. 调用 Spring AI 生成推荐理由
            String aiResponse = chatClient.prompt()
                .toolContext(Map.of("userId", userId))
                .system("""
                    你是一位专业的学习规划导师，擅长根据用户的学习情况给出个性化的学习建议。
                    请根据用户的学习画像和推荐技能列表，为每个推荐技能生成一条简短的推荐理由（50字以内）。
                    返回JSON格式：
                    {
                      "reasons": [
                        {"skill_name": "技能名称", "reason": "推荐理由"}
                      ]
                    }
                    """)
                .user(prompt)
                .call()
                .content();

            // 5. 解析AI响应并填充推荐理由
            parseAiResponse(recommendations, aiResponse);
            log.info("AI推荐理由生成成功");

        } catch (Exception e) {
            log.warn("AI推荐理由生成失败，使用默认理由", e);
            // AI调用失败时保持默认理由
        }

        return recommendations;
    }

    private LearningPathVO createRecommendation(Skill skill, int priority, String defaultReason) {
        LearningPathVO vo = new LearningPathVO();
        vo.setSkillId(skill.getId());
        vo.setSkillName(skill.getName());
        vo.setCategory(skill.getCategory());
        vo.setDescription(skill.getDescription());
        vo.setLevel(skill.getLevel());
        vo.setPriority(priority);
        vo.setReason(defaultReason);
        return vo;
    }

    private String buildAiPrompt(Long userId, List<UserSkill> masteredSkills,
                                 List<LearningRecord> learningRecords,
                                 List<LearningPathVO> recommendations) {
        StringBuilder prompt = new StringBuilder();
        prompt.append("用户学习画像：\n");

        // 已掌握技能
        if (masteredSkills.isEmpty()) {
            prompt.append("- 已掌握技能：无\n");
        } else {
            prompt.append("- 已掌握技能：");
            List<String> skillNames = masteredSkills.stream()
                .map(us -> {
                    Skill skill = skillMapper.selectById(us.getSkillId());
                    return skill != null ? skill.getName() + "(level=" + us.getLevel() + ")" : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            prompt.append(String.join(", ", skillNames)).append("\n");
        }

        // 学习中技能
        if (learningRecords.isEmpty()) {
            prompt.append("- 学习中技能：无\n");
        } else {
            prompt.append("- 学习中技能：");
            List<String> skillNames = learningRecords.stream()
                .map(lr -> {
                    Skill skill = skillMapper.selectById(lr.getSkillId());
                    return skill != null ? skill.getName() : null;
                })
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
            prompt.append(String.join(", ", skillNames)).append("\n");
        }

        // 推荐技能列表
        prompt.append("\n推荐技能列表：\n");
        for (int i = 0; i < recommendations.size(); i++) {
            LearningPathVO rec = recommendations.get(i);
            prompt.append(String.format("%d. %s (level=%d, %s)\n",
                i + 1, rec.getSkillName(), rec.getLevel(), rec.getCategory()));
        }

        prompt.append("\n请为每个推荐技能生成推荐理由，返回JSON格式。");
        return prompt.toString();
    }

    private void parseAiResponse(List<LearningPathVO> recommendations, String aiResponse) {
        try {
            // 简单的JSON解析（实际项目中建议使用Jackson）
            // 这里使用正则提取
            for (LearningPathVO vo : recommendations) {
                String skillName = vo.getSkillName();
                // 查找包含该技能名称的reason
                int idx = aiResponse.indexOf("\"" + skillName + "\"");
                if (idx != -1) {
                    int reasonStart = aiResponse.indexOf("\"reason\"", idx);
                    if (reasonStart != -1) {
                        int valueStart = aiResponse.indexOf("\"", reasonStart + 8);
                        int valueEnd = aiResponse.indexOf("\"", valueStart + 1);
                        if (valueStart != -1 && valueEnd != -1) {
                            String reason = aiResponse.substring(valueStart + 1, valueEnd);
                            vo.setReason(reason);
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.warn("解析AI响应失败", e);
        }
    }
}
