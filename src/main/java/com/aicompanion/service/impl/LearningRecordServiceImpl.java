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

    @Override
    public LearningStatsVO getUserLearningStats(Long userId) {
        LearningStatsVO stats = learningRecordMapper.getUserLearningStats(userId);
        if (stats == null) {
            stats = new LearningStatsVO();
        }

        stats.setActivePlansCount(0);
        Integer consecutiveDays = learningRecordMapper.selectConsecutiveDays(userId);
        stats.setConsecutiveDays(consecutiveDays != null ? consecutiveDays : 0);

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
        learningRecordMapper.updateById(record);
        log.debug("学习心跳: recordId={}, totalSeconds={}", recordId, newSeconds);
    }

    @Override
    public LearningRecordVO endLearning(Long recordId, Integer clientStudySeconds) {
        LearningRecord record = learningRecordMapper.selectById(recordId);
        if (record == null) {
            throw new BusinessException(404, "学习记录不存在");
        }

        record.setStatus(2);
        record.setCompleteTime(LocalDateTime.now());
        record.setProgress(100);
        
        // 取前端本地计时和后端心跳计时的最大值，确保不足30秒的学习也能被记录
        int dbSeconds = record.getStudySeconds() != null ? record.getStudySeconds() : 0;
        int finalSeconds = Math.max(dbSeconds, clientStudySeconds != null ? clientStudySeconds : 0);
        record.setStudySeconds(finalSeconds);
        
        learningRecordMapper.updateById(record);

        log.info("结束学习: recordId={}, dbSeconds={}, clientSeconds={}, finalSeconds={}", 
                recordId, dbSeconds, clientStudySeconds, finalSeconds);
        return toVO(record);
    }

    private LearningRecordVO toVO(LearningRecord record) {
        LearningRecordVO vo = new LearningRecordVO();
        BeanUtils.copyProperties(record, vo);
        return vo;
    }

    /**
     * 更新 user_skill 状态为"学习中"
     * 只要用户开始学习某个技能，就创建或更新 user_skill 记录为学习中状态
     */
    private void updateUserSkillToLearning(Long userId, Long skillId) {
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
            userSkill.setLevel(0);
            userSkill.setStatus(1); // 学习中
            userSkillMapper.insert(userSkill);
            log.info("创建学习中技能: userId={}, skillId={}", userId, skillId);
        } else if (userSkill.getStatus() == 0) {
            // 从"未开始"更新为"学习中"
            userSkill.setStatus(1);
            userSkillMapper.updateById(userSkill);
            log.info("更新技能为学习中: userId={}, skillId={}", userId, skillId);
        }
        // 如果已经是"已点亮"(status=2)，不降级
    }
}
