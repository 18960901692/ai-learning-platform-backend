package com.aicompanion.controller;

import com.aicompanion.common.response.PageResult;
import com.aicompanion.common.response.Result;
import com.aicompanion.model.entity.ChatMessage;
import com.aicompanion.model.entity.ChatSession;
import com.aicompanion.model.entity.User;
import com.aicompanion.model.vo.AdminChatSessionVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.aicompanion.mapper.ChatMessageMapper;
import com.aicompanion.mapper.ChatSessionMapper;
import com.aicompanion.mapper.UserMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 管理后台 AI 会话记录 Controller
 */
@Tag(name = "管理后台-AI会话记录", description = "管理后台查看用户 AI 对话记录（不含考核）")
@RestController
@RequestMapping("/admin/chat-sessions")
@RequiredArgsConstructor
public class AdminChatController {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserMapper userMapper;

    @Operation(summary = "分页查询会话列表", description = "管理员分页查询所有 AI 会话记录，排除考核类型，支持按用户名搜索和类型筛选")
    @GetMapping("/list")
    public Result<PageResult<AdminChatSessionVO>> getSessionList(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String agentType) {

        LambdaQueryWrapper<ChatSession> wrapper = new LambdaQueryWrapper<>();
        // 排除考核类型
        wrapper.ne(ChatSession::getAgentType, "EXAM");
        // 按类型筛选
        if (agentType != null && !agentType.isBlank()) {
            wrapper.eq(ChatSession::getAgentType, agentType);
        }
        wrapper.orderByDesc(ChatSession::getCreateTime);

        // 按用户名搜索
        if (keyword != null && !keyword.isBlank()) {
            List<User> matchedUsers = userMapper.selectList(
                    new LambdaQueryWrapper<User>()
                            .like(User::getUsername, keyword)
                            .or()
                            .like(User::getNickname, keyword)
            );
            if (matchedUsers.isEmpty()) {
                return Result.success(PageResult.of(0, List.of(), page, pageSize));
            }
            List<Long> userIds = matchedUsers.stream().map(User::getId).toList();
            wrapper.in(ChatSession::getUserId, userIds);
        }

        Page<ChatSession> pageParam = new Page<>(page, pageSize);
        IPage<ChatSession> result = chatSessionMapper.selectPage(pageParam, wrapper);

        List<AdminChatSessionVO> voList = result.getRecords().stream().map(session -> {
            AdminChatSessionVO vo = new AdminChatSessionVO();
            vo.setId(session.getId());
            vo.setTitle(session.getTitle());
            vo.setAgentType(session.getAgentType());
            vo.setCreateTime(session.getCreateTime());
            vo.setUpdateTime(session.getUpdateTime());

            User user = userMapper.selectById(session.getUserId());
            if (user != null) {
                vo.setUsername(user.getUsername());
                vo.setNickname(user.getNickname());
            }

            // 统计消息数
            long msgCount = chatMessageMapper.selectCount(
                    new LambdaQueryWrapper<ChatMessage>()
                            .eq(ChatMessage::getSessionId, session.getId())
            );
            vo.setMessageCount((int) msgCount);

            return vo;
        }).toList();

        return Result.success(PageResult.of((int) result.getTotal(), voList, page, pageSize));
    }

    @Operation(summary = "获取会话消息列表", description = "根据会话ID获取该会话的所有对话消息")
    @GetMapping("/{id}/messages")
    public Result<List<ChatMessage>> getSessionMessages(@PathVariable Long id) {
        List<ChatMessage> messages = chatMessageMapper.selectList(
                new LambdaQueryWrapper<ChatMessage>()
                        .eq(ChatMessage::getSessionId, id)
                        .orderByAsc(ChatMessage::getCreateTime)
        );
        return Result.success(messages);
    }
}
