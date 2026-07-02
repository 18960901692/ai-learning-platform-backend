package com.aicompanion.tool;

import com.aicompanion.mapper.LearningRecordMapper;
import com.aicompanion.model.entity.LearningRecord;
import com.aicompanion.model.vo.LearningRecordInfo;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.tool.annotation.Tool;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

/**
 * 学习记录查询工具 - 查询用户的学习记录（学习时长、完成进度）
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningRecordTool {

    private final LearningRecordMapper learningRecordMapper;

    /**
     * 当前请求的用户ID（由 AiChatServiceImpl 在每次调用前设置）
     */
    private volatile Long currentUserId;

    public void setCurrentUserId(Long userId) {
        this.currentUserId = userId;
    }

    @Tool(description = "查询当前登录用户的学习记录。返回总学习时长、学习中的技能数、已完成的技能数、连续打卡天数、各技能学习详情。")
    public LearningRecordInfo getLearningRecords() {
        Long userId = currentUserId;
        log.info("LearningRecordTool 被调用: userId={}", userId);

        if (userId == null) {
            return new LearningRecordInfo(0, 0, 0, 0, List.of());
        }

        // 查询所有学习记录
        List<LearningRecord> records = learningRecordMapper.selectList(
                new LambdaQueryWrapper<LearningRecord>()
                        .eq(LearningRecord::getUserId, userId)
                        .eq(LearningRecord::getDeleted, 0)
                        .orderByDesc(LearningRecord::getLastStudyTime)
        );

        // 统计总览
        int totalStudySeconds = 0;
        int studyingCount = 0;
        int completedCount = 0;

        List<LearningRecordInfo.SkillLearningDetail> details = new ArrayList<>();

        for (LearningRecord record : records) {
            totalStudySeconds += record.getStudySeconds() != null ? record.getStudySeconds() : 0;

            int status = record.getStatus() != null ? record.getStatus() : 0;
            String statusText = switch (status) {
                case 2 -> "已完成";
                case 1 -> "学习中";
                default -> "未开始";
            };

            if (status == 1) {
                studyingCount++;
            } else if (status == 2) {
                completedCount++;
            }

            details.add(new LearningRecordInfo.SkillLearningDetail(
                    record.getSkillName(),
                    record.getProgress() != null ? record.getProgress() : 0,
                    record.getStudySeconds() != null ? record.getStudySeconds() : 0,
                    statusText
            ));
        }

        // 查询连续打卡天数
        Integer consecutiveDays = learningRecordMapper.selectConsecutiveDays(userId);

        return new LearningRecordInfo(
                totalStudySeconds,
                studyingCount,
                completedCount,
                consecutiveDays != null ? consecutiveDays : 0,
                details
        );
    }
}
