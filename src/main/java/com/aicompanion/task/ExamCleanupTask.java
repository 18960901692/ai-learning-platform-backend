package com.aicompanion.task;

import com.aicompanion.service.ExamService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 考核记录定时清理任务
 * 每天凌晨 3:00 清理超过 24 小时未提交的僵尸考核记录
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class ExamCleanupTask {

    private final ExamService examService;

    /**
     * 每天凌晨 3:00 执行
     */
    @Scheduled(cron = "0 0 3 * * ?")
    public void cleanAbandonedExams() {
        log.info("开始执行僵尸考核记录清理任务...");
        try {
            int cleaned = examService.cleanAbandonedExams();
            log.info("僵尸考核记录清理任务完成: 清理数量={}", cleaned);
        } catch (Exception e) {
            log.error("僵尸考核记录清理任务失败", e);
        }
    }
}
