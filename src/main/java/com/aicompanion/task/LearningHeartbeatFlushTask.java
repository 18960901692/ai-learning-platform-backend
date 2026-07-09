package com.aicompanion.task;

import com.aicompanion.service.LearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * 学习心跳缓冲定时刷盘任务
 *
 * <p>心跳写入只累加 Redis 计数器，由本任务每 5 分钟批量刷盘到 MySQL，
 * 将 DB 写入频率从「每 30 秒/次/用户」降低为「每 5 分钟批量一次」，
 * 大幅降低 MySQL 写入压力。</p>
 *
 * <p>数据安全性保障：
 * <ul>
 *   <li>正常退出：endLearning 触发 drain 立即刷盘</li>
 *   <li>异常断电/断网：Redis 持久化（RDB/AOF）+ 本任务兜底，最大丢失窗口 5 分钟</li>
 * </ul>
 * </p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class LearningHeartbeatFlushTask {

    private final LearningRecordService learningRecordService;

    /**
     * 每 5 分钟执行一次（应用启动完成后开始）
     */
    @Scheduled(fixedDelay = 5 * 60 * 1000L, initialDelay = 5 * 60 * 1000L)
    public void flushHeartbeatBuffer() {
        try {
            int flushed = learningRecordService.flushBufferedHeartbeats();
            if (flushed > 0) {
                log.info("心跳缓冲刷盘任务完成: 刷盘记录数={}", flushed);
            }
        } catch (Exception e) {
            log.error("心跳缓冲刷盘任务失败", e);
        }
    }
}
