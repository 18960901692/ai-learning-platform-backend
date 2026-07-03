package com.aicompanion.service.impl;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.mapper.AdminExamRecordMapper;
import com.aicompanion.model.vo.AdminExamRecordVO;
import com.aicompanion.service.AdminExamRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台考核记录 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExamRecordServiceImpl implements AdminExamRecordService {

    private final AdminExamRecordMapper adminExamRecordMapper;

    @Override
    public PageResult<AdminExamRecordVO> getExamRecords(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        List<AdminExamRecordVO> records = adminExamRecordMapper.selectExamRecords(keyword, offset, pageSize);
        long total = adminExamRecordMapper.countExamRecords(keyword);

        log.info("查询考核记录: page={}, pageSize={}, keyword={}, total={}", page, pageSize, keyword, total);

        return PageResult.of(total, records, page, pageSize);
    }
}
