package com.aicompanion.service;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.model.vo.AdminExamRecordVO;

/**
 * 管理后台考核记录 Service
 */
public interface AdminExamRecordService {

    /**
     * 分页查询考核记录
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词(用户名)
     * @return 分页结果
     */
    PageResult<AdminExamRecordVO> getExamRecords(int page, int pageSize, String keyword);
}
