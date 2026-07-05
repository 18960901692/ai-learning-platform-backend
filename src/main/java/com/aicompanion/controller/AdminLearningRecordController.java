package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.model.vo.AdminLearningRecordVO;
import com.aicompanion.service.AdminLearningRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理后台学习记录 Controller
 */
@Tag(name = "管理后台-学习记录", description = "管理后台学习记录查询接口")
@RestController
@RequestMapping("/admin/learning-records")
@RequiredArgsConstructor
public class AdminLearningRecordController {

    private final AdminLearningRecordService adminLearningRecordService;

    @Operation(summary = "分页查询学习记录", description = "管理员分页查询所有用户的学习记录，支持按用户名/昵称搜索和状态筛选")
    @GetMapping("/list")
    public Result<PageResult<AdminLearningRecordVO>> getLearningRecords(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer status) {
        return Result.success(adminLearningRecordService.getLearningRecords(page, pageSize, keyword, status));
    }
}
