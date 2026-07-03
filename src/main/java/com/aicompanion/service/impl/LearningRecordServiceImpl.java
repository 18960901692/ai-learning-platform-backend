package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.entity.LearningRecord;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.UserSkill;
import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.model.vo.LearningStatsVO;
import com.aicompanion.service.CheckInService;
import com.aicompanion.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 学习记录服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LearningRecordServiceImpl implements LearningRecordService {

    private final LearningRecordMapper learningRecordMapper;
    private final SkillMapper skillMapper;
    private final UserSkillMapper userSkillMapper;
    private final CheckInService checkInService;

    @Override
    public LearningStatsVO getUserLearningStats(Long userId) {
        LearningStatsVO stats = learningRecordMapper.getUserLearningStats(userId);
        if (stats == null) {
            stats = new LearningStatsVO();
        }

        stats.setActivePlansCount(0);
        int consecutiveDays = checkInService.getConsecutiveDays(userId);
        stats.setConsecutiveDays(consecutiveDays);

        log.info("获取用户学习统计数据成功: userId={}, stats={}", userId, stats);
        return stats;
    }

    @Override
    public List<LearningRecordVO> getUserLearningRecords(Long userId) {
        List<LearningRecord> records = learningRecordMapper.selectByUserId(userId);
        return records.stream()
                .map(this::toVO)
                .collect(Collectors.toList());
    }

    @Override
    public Long startLearning(Long userId, Long skillId) {
        Skill skill = skillMapper.selectById(skillId);
        if (skill == null) {
            throw new BusinessException(404, "技能不存在");
        }

        // 开始学习即标记今日打卡（放在所有 return 之前，确保所有路径都打卡）
        checkInService.checkIn(userId);

        // 查找是否有进行中的学习记录
        LearningRecord record = learningRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getSkillId, skillId)
                        .eq(LearningRecord::getStatus, 1)
                        .last("LIMIT 1")
        );

        if (record != null) {
            return record.getId();
        }

        // 查找已有的记录（已完成或未开始）
        record = learningRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        if (record == null) {
            record = new LearningRecord();
            record.setUserId(userId);
            record.setSkillId(skillId);
            record.setSkillName(skill.getName());
            record.setSourceType("AI_CHAT");
            record.setProgress(0);
            record.setStudySeconds(0);
            record.setStatus(1);
            record.setFirstStudyTime(LocalDateTime.now());
            record.setLastStudyTime(LocalDateTime.now());
            learningRecordMapper.insert(record);
        } else {
            record.setStatus(1);
            record.setLastStudyTime(LocalDateTime.now());
            if (record.getFirstStudyTime() == null) {
                record.setFirstStudyTime(LocalDateTime.now());
            }
            learningRecordMapper.updateById(record);
        }

        log.info("开始学习技能: userId={}, skillId={}, recordId={}", userId, skillId, record.getId());
        
        // 同步更新 user_skill 状态为"学习中"
        updateUserSkillToLearning(userId, skillId);
        
        return record.getId();
    }

    @Override
    public void heartbeat(Long recordId) {
        LearningRecord record = learningRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "学习记录不存在");
        }

        int newSeconds = (record.getStudySeconds() != null ? record.getStudySeconds() : 0) + 30;
        record.setStudySeconds(newSeconds);
        record.setLastStudyTime(LocalDateTime.now());

        // 根据点亮状态动态计算进度
        int progress = calculateProgress(record.getUserId(), record.getSkillId(), newSeconds);
        record.setProgress(progress);

        learningRecordMapper.updateById(record);
        checkInService.checkIn(record.getUserId());
        log.debug("学习心跳: recordId={}, totalSeconds={}, progress={}", recordId, newSeconds, progress);
    }

    @Override
    public LearningRecordVO endLearning(Long recordId, Integer clientStudySeconds) {
        LearningRecord record = learningRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "学习记录不存在");
        }

        // 保持 status=1（学习中），不改为 2，只有考核通过后才标记为已完成
        // record.setStatus(2);  // 删除此行

        // 取前端本地计时和后端心跳计时的最大值，确保不足30秒的学习也能被记录
        int dbSeconds = record.getStudySeconds() != null ? record.getStudySeconds() : 0;
        int finalSeconds = Math.max(dbSeconds, clientStudySeconds != null ? clientStudySeconds : 0);
        record.setStudySeconds(finalSeconds);
        record.setLastStudyTime(LocalDateTime.now());

        // 根据点亮状态动态计算进度
        int progress = calculateProgress(record.getUserId(), record.getSkillId(), finalSeconds);
        record.setProgress(progress);

        learningRecordMapper.updateById(record);

        // 根据最终学习时长更新 user_skill 的 level
        updateUserSkillLevel(record.getUserId(), record.getSkillId(), finalSeconds);

        log.info("结束学习: recordId={}, dbSeconds={}, clientSeconds={}, finalSeconds={}, progress={}",
                recordId, dbSeconds, clientStudySeconds, finalSeconds, progress);
        return toVO(record);
    }

    /**
     * 根据学习时长更新 user_skill 的 level（只升不降）
     */
    private void updateUserSkillLevel(Long userId, Long skillId, int studySeconds) {
        int level = calculateLevelByStudySeconds(studySeconds);

        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        if (userSkill != null && userSkill.getStatus() != 2) {
            // 只升不降，已点亮的不降级
            if (level > userSkill.getLevel()) {
                userSkill.setLevel(level);
                userSkillMapper.updateById(userSkill);
                log.info("更新技能等级: userId={}, skillId={}, level={}, studySeconds={}", userId, skillId, level, studySeconds);
            }
        }
    }

    private LearningRecordVO toVO(LearningRecord record) {
        LearningRecordVO vo = new LearningRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    /**
     * 根据学习时长计算掌握等级
     */
    private int calculateLevelByStudySeconds(int studySeconds) {
        if (studySeconds == 0) return 0;       // < 1分钟: 未开始
        if (studySeconds < 300) return 1;      // 1-5分钟: 入门
        if (studySeconds < 900) return 2;      // 5-15分钟: 基础
        if (studySeconds < 1800) return 3;     // 15-30分钟: 熟练
        if (studySeconds < 3600) return 4;     // 30-60分钟: 精通
        return 5;                               // > 60分钟: 专家
    }

    /**
     * 根据等级计算进度百分比
     * level 0-5 对应 0%-100%
     */
    private int calculateProgressByLevel(int level) {
        if (level <= 0) return 0;
        if (level >= 5) return 100;
        return level * 20;  // level 1=20%, 2=40%, 3=60%, 4=80%, 5=100%
    }

    /**
     * 计算学习进度
     * 已点亮：100%
     * 未点亮：按等级计算（level * 20）
     */
    private int calculateProgress(Long userId, Long skillId, int studySeconds) {
        // 查询是否已点亮
        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        // 已点亮，进度 100%
        if (userSkill != null && userSkill.getStatus() == 2) {
            return 100;
        }

        // 未点亮，按等级计算
        int level = calculateLevelByStudySeconds(studySeconds);
        return calculateProgressByLevel(level);
    }

    /**
     * 更新 user_skill 状态为"学习中"
     * 只要用户开始学习某个技能，就创建或更新 user_skill 记录为学习中状态
     * 同时根据累计学习时长更新 level
     */
    private void updateUserSkillToLearning(Long userId, Long skillId) {
        // 先获取当前学习时长
        LearningRecord record = learningRecordMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getSkillId, skillId)
                        .last("LIMIT 1")
        );
        int studySeconds = record != null && record.getStudySeconds() != null ? record.getStudySeconds() : 0;
        int level = calculateLevelByStudySeconds(studySeconds);

        UserSkill userSkill = userSkillMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<UserSkill>()
                        .eq(UserSkill::getUserId, userId)
                        .eq(UserSkill::getSkillId, skillId)
                        .last("LIMIT 1")
        );

        if (userSkill == null) {
            // 创建"学习中"记录
            userSkill = new UserSkill();
            userSkill.setUserId(userId);
            userSkill.setSkillId(skillId);
            userSkill.setLevel(level);
            userSkill.setStatus(1); // 学习中
            userSkillMapper.insert(userSkill);
            log.info("创建学习中技能: userId={}, skillId={}, level={}, studySeconds={}", userId, skillId, level, studySeconds);
        } else if (userSkill.getStatus() == 0) {
            // 从"未开始"更新为"学习中"，同时更新 level
            userSkill.setStatus(1);
            userSkill.setLevel(Math.max(userSkill.getLevel(), level));
            userSkillMapper.updateById(userSkill);
            log.info("更新技能为学习中: userId={}, skillId={}, level={}, studySeconds={}", userId, skillId, userSkill.getLevel(), studySeconds);
        } else if (userSkill.getStatus() == 1) {
            // 已经是"学习中"，根据最新学习时长更新 level（只升不降）
            userSkill.setLevel(Math.max(userSkill.getLevel(), level));
            userSkillMapper.updateById(userSkill);
            log.info("更新技能等级: userId={}, skillId={}, level={}, studySeconds={}", userId, skillId, userSkill.getLevel(), studySeconds);
        }
        // 如果已经是"已点亮"(status=2)，不降级
    }
}
