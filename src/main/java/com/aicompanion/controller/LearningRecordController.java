package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.EndLearningDTO;
import com.aicompanion.model.dto.HeartbeatDTO;
import com.aicompanion.model.dto.StartExamDTO;
import com.aicompanion.model.vo.LearningRecordVO;
import com.aicompanion.service.LearningRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 学习记录控制器
 */
@Tag(name = "学习记录", description = "学习计时相关接口")
@RestController
@RequestMapping("/learning")
@RequiredArgsConstructor
public class LearningRecordController {

    private final LearningRecordService learningRecordService;

    @Operation(summary = "开始学习", description = "开始学习某技能，返回学习记录ID")
    @PostMapping("/start")
    public Result<Long> startLearning(@Valid @RequestBody StartExamDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        Long recordId = learningRecordService.startLearning(userId, dto.getSkillId());
        return Result.success("开始学习", recordId);
    }

    @Operation(summary = "学习心跳", description = "前端每30秒发送一次，累加学习时长")
    @PostMapping("/heartbeat")
    public Result<Void> heartbeat(@Valid @RequestBody HeartbeatDTO dto) {
        learningRecordService.heartbeat(dto.getRecordId());
        return Result.success("心跳成功", null);
    }

    @Operation(summary = "结束学习", description = "结束学习，结算时长")
    @PostMapping("/end/{recordId}")
    public Result<LearningRecordVO> endLearning(@PathVariable Long recordId,
                                                  @Valid @RequestBody EndLearningDTO dto) {
        System.out.println("=== endLearning 被调用 ===");
        System.out.println("recordId: " + recordId);
        System.out.println("dto.studySeconds: " + dto.getStudySeconds());
        LearningRecordVO vo = learningRecordService.endLearning(recordId, dto.getStudySeconds());
        return Result.success("学习完成", vo);
    }
}
