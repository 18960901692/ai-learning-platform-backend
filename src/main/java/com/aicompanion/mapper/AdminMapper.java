package com.aicompanion.mapper;

import com.aicompanion.model.entity.Admin;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 管理员 Mapper
 */
@Mapper
public interface AdminMapper extends BaseMapper<Admin> {
}
