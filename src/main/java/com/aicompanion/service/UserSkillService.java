package com.aicompanion.service;

import com.aicompanion.model.vo.UserSkillVO;

import java.util.List;

/**
 * 用户技能服务接口
 */
public interface UserSkillService {

    /**
     * 获取用户技能掌握情况列表
     */
    List<UserSkillVO> getUserSkills(Long userId);
}
