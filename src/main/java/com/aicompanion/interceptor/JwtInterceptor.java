package com.aicompanion.interceptor;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.model.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：校验 Token 有效性，统一从 user 表认证，通过 role 区分权限
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = SecurityUtil.extractToken(request);

        // 对于 AI 对话接口，允许未登录访问（Token 为空时放行）
        String uri = request.getRequestURI();
        if (uri.startsWith("/ai/chat/")) {
            if (token == null || token.isBlank()) {
                return true; // 放行，不设置 userId
            }
            // 有 Token 则验证，但不强制要求
            if (!jwtUtil.validateToken(token)) {
                return true; // Token 无效也放行
            }
            Long userId = jwtUtil.getUserId(token);
            String userType = jwtUtil.getUserType(token);
            User user = userMapper.selectById(userId);
            if (user != null && user.getStatus() == 1) {
                SecurityUtil.setCurrentUser(request, userId, userType);
            }
            return true;
        }

        if (token == null || token.isBlank()) {
            writeError(response, 401, "未登录，请先登录");
            return false;
        }

        if (!jwtUtil.validateToken(token)) {
            writeError(response, 401, "Token 已过期，请重新登录");
            return false;
        }

        // 将用户信息存入 request
        Long userId = jwtUtil.getUserId(token);
        String userType = jwtUtil.getUserType(token);

        // 统一从 user 表验证用户是否存在且状态正常
        User user = userMapper.selectById(userId);
        if (user == null || user.getStatus() != 1) {
            writeError(response, 401, "账号不存在或已被禁用");
            return false;
        }

        // 校验 role 与 userType 是否匹配（兼容老数据 role 为 NULL 的情况，默认视为 USER）
        String userRole = user.getRole() != null ? user.getRole() : "USER";
        if ("ADMIN".equals(userType) && !"ADMIN".equals(userRole)) {
            writeError(response, 403, "该账号为学生账号，请使用学生端登录");
            return false;
        }
        if ("USER".equals(userType) && !"USER".equals(userRole)) {
            writeError(response, 403, "该账号为管理员账号，请使用管理后台登录");
            return false;
        }

        SecurityUtil.setCurrentUser(request, userId, userType);

        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        Result<Void> result = Result.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
