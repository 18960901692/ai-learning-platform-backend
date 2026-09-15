package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.StartExamDTO;
import com.aicompanion.model.vo.ExamSessionVO;
import com.aicompanion.model.vo.StartExamVO;
import com.aicompanion.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "考核管理", description = "技能考核相关接口")
@Slf4j
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "开始考核", description = "开始技能考核，返回会话ID")
    @PostMapping("/start")
    public Result<StartExamVO> startExam(@Valid @RequestBody StartExamDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        StartExamVO result = examService.startExam(userId, dto.getSkillId());
        return Result.success("开始考核", result);
    }

    @Operation(summary = "获取考核结果", description = "根据考核会话ID获取详细结果")
    @GetMapping("/result/{sessionId}")
    public Result<ExamSessionVO> getExamResult(@PathVariable Long sessionId) {
        ExamSessionVO result = examService.getExamResult(sessionId);
        return Result.success(result);
    }
}
