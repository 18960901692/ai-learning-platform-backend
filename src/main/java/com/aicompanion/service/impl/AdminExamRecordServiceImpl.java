package com.aicompanion.service.impl;

import com.aicompanion.common.exception.BusinessException;
import com.aicompanion.common.response.PageResult;
import com.aicompanion.mapper.AdminExamRecordMapper;
import com.aicompanion.mapper.ExamSessionMapper;
import com.aicompanion.mapper.SkillMapper;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.model.entity.ExamSession;
import com.aicompanion.model.entity.Skill;
import com.aicompanion.model.entity.User;
import com.aicompanion.model.vo.AdminExamDetailVO;
import com.aicompanion.model.vo.AdminExamRecordVO;
import com.aicompanion.service.AdminExamRecordService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台考核记录 Service 实现
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminExamRecordServiceImpl implements AdminExamRecordService {

    private final AdminExamRecordMapper adminExamRecordMapper;
    private final ExamSessionMapper examSessionMapper;
    private final UserMapper userMapper;
    private final SkillMapper skillMapper;

    @Override
    public PageResult<AdminExamRecordVO> getExamRecords(int page, int pageSize, String keyword) {
        int offset = (page - 1) * pageSize;
        List<AdminExamRecordVO> records = adminExamRecordMapper.selectExamRecords(keyword, offset, pageSize);
        long total = adminExamRecordMapper.countExamRecords(keyword);

        log.info("查询考核记录: page={}, pageSize={}, keyword={}, total={}", page, pageSize, keyword, total);

        return PageResult.of(total, records, page, pageSize);
    }

    @Override
    public AdminExamDetailVO getExamDetail(Long id) {
        ExamSession session = examSessionMapper.selectById(id);
        if (session == null) {
            throw new BusinessException(404, "考核记录不存在");
        }

        User user = userMapper.selectById(session.getUserId());
        Skill skill = skillMapper.selectById(session.getSkillId());

        AdminExamDetailVO vo = new AdminExamDetailVO();
        vo.setId(session.getId());
        vo.setUsername(user != null ? user.getUsername() : "");
        vo.setNickname(user != null ? user.getNickname() : "");
        vo.setSkillName(skill != null ? skill.getName() : "");
        vo.setScore(session.getTotalScore());
        vo.setPassScore(session.getPassScore());
        vo.setStatus("PASSED".equals(session.getStatus()) ? "通过" : "未通过");
        vo.setAiFeedback(session.getAiFeedback());
        vo.setStartTime(session.getStartTime());
        vo.setEndTime(session.getEndTime());

        if (session.getStartTime() != null && session.getEndTime() != null) {
            long minutes = Duration.between(session.getStartTime(), session.getEndTime()).toMinutes();
            vo.setDuration(minutes + "分钟");
        }

        return vo;
    }
}
