package com.aicompanion.service.impl;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.mapper.AdminLearningRecordMapper;
import com.aicompanion.model.vo.AdminLearningRecordVO;
import com.aicompanion.service.AdminLearningRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 管理后台学习记录 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminLearningRecordServiceImpl implements AdminLearningRecordService {

    private final AdminLearningRecordMapper adminLearningRecordMapper;

    @Override
    public PageResult<AdminLearningRecordVO> getLearningRecords(int page, int pageSize, String keyword, Integer status) {
        int offset = (page - 1) * pageSize;
        List<AdminLearningRecordVO> records = adminLearningRecordMapper.selectLearningRecords(keyword, status, offset, pageSize);
        long total = adminLearningRecordMapper.countLearningRecords(keyword, status);

        log.info("查询学习记录: page={}, pageSize={}, keyword={}, status={}, total={}", page, pageSize, keyword, status, total);

        return PageResult.of(total, records, page, pageSize);
    }
}
