package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.model.vo.AdminExamRecordVO;
import com.aicompanion.service.AdminExamRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台考核记录 Controller
 */
@Tag(name = "管理后台-考核记录", description = "管理后台考核记录查询接口")
@RestController
@RequestMapping("/admin/exam-records")
@RequiredArgsConstructor
public class AdminExamRecordController {

    private final AdminExamRecordService adminExamRecordService;

    @Operation(summary = "分页查询考核记录", description = "管理员分页查询所有考核记录,支持按用户名搜索")
    @GetMapping("/list")
    public Result<PageResult<AdminExamRecordVO>> getExamRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword) {
        return Result.success(adminExamRecordService.getExamRecords(page, pageSize, keyword));
    }
}
