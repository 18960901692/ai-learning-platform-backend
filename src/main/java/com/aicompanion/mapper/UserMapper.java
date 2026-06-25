package com.aicompanion.mapper;

import com.aicompanion.model.entity.User;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

/**
 * 用户 Mapper
 */
@Mapper
public interface UserMapper extends BaseMapper<User> {

    /**
     * 动态搜索用户（支持角色筛选 + 关键词模糊搜索）
     *
     * @param keyword 关键词（用户名/昵称/邮箱）
     * @param role    角色（USER/ADMIN）
     * @return 用户列表
     */
    List<User> searchUsers(@Param("keyword") String keyword, @Param("role") String role);
}
