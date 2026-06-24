package com.aicompanion.service.impl;

import com.aicompanion.mapper.UserSkillMapper;
import com.aicompanion.model.vo.UserSkillVO;
import com.aicompanion.service.UserSkillService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * 用户技能服务实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class UserSkillServiceImpl implements UserSkillService {

    private final UserSkillMapper userSkillMapper;

    @Override
    public List<UserSkillVO> getUserSkills(Long userId) {
        List<UserSkillVO> skills = userSkillMapper.getUserSkills(userId);
        log.info("获取用户技能掌握情况: userId={}, count={}", userId, skills.size());
        return skills;
    }
}
