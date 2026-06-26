package com.aicompanion.controller;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.model.dto.DifyGradeDTO;
import com.aicompanion.model.dto.SubmitAnswerDTO;
import com.aicompanion.model.vo.ExamSessionVO;
import com.aicompanion.model.vo.StartExamVO;
import com.aicompanion.service.ExamService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@Tag(name = "考核管理", description = "技能考核相关接口")
@RestController
@RequestMapping("/exam")
@RequiredArgsConstructor
public class ExamController {

    private final ExamService examService;

    @Operation(summary = "开始考核", description = "开始技能考核，返回会话ID和题目列表（不含答案）")
    @PostMapping("/start")
    public Result<StartExamVO> startExam(@Valid @RequestBody com.aicompanion.model.dto.StartExamDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        StartExamVO result = examService.startExam(userId, dto.getSkillId());
        return Result.success("开始考核", result);
    }

    @Operation(summary = "提交答案并评分", description = "用户答完所有题后一次性提交，系统自动评分并返回结果")
    @PostMapping("/submit")
    public Result<ExamSessionVO> submitAndGrade(@Valid @RequestBody SubmitAnswerDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> questionIds = dto.getAnswers().stream()
                .map(SubmitAnswerDTO.SingleAnswerDTO::getQuestionId)
                .collect(Collectors.toList());
        List<String> answers = dto.getAnswers().stream()
                .map(SubmitAnswerDTO.SingleAnswerDTO::getUserAnswer)
                .collect(Collectors.toList());
        ExamSessionVO result = examService.submitAndGrade(userId, dto.getSessionId(), questionIds, answers);
        return Result.success("考核完成", result);
    }

    @Operation(summary = "获取考核结果", description = "根据考核会话ID获取详细结果")
    @GetMapping("/result/{sessionId}")
    public Result<ExamSessionVO> getExamResult(@PathVariable Long sessionId) {
        ExamSessionVO result = examService.getExamResult(sessionId);
        return Result.success(result);
    }

    @Operation(summary = "保存Dify阅卷结果", description = "鸿蒙端调用Dify阅卷后，将评分结果传给后端保存")
    @PostMapping("/grade")
    public Result<ExamSessionVO> saveDifyGrade(@Valid @RequestBody DifyGradeDTO dto) {
        Long userId = SecurityUtil.getCurrentUserId();
        List<Long> questionIds = null;
        List<String> answers = null;
        if (dto.getAnswers() != null) {
            questionIds = dto.getAnswers().stream().map(DifyGradeDTO.SingleAnswerDTO::getQuestionId).collect(Collectors.toList());
            answers = dto.getAnswers().stream().map(DifyGradeDTO.SingleAnswerDTO::getUserAnswer).collect(Collectors.toList());
        }
        ExamSessionVO result = examService.saveDifyGrade(userId, dto.getSessionId(), dto.getText(), dto.getScore(), questionIds, answers);
        return Result.success("评分已保存", result);
    }
}
