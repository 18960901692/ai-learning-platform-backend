package com.aicompanion.service;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.model.vo.AdminLearningRecordVO;

/**
 * 管理后台学习记录 Service
 */
public interface AdminLearningRecordService {

    /**
     * 分页查询学习记录
     *
     * @param page     页码
     * @param pageSize 每页大小
     * @param keyword  搜索关键词（用户名/昵称）
     * @return 分页结果
     */
    PageResult<AdminLearningRecordVO> getLearningRecords(int page, int pageSize, String keyword);
}
