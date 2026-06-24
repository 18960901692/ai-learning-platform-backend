package com.aicompanion.interceptor;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.common.util.SecurityUtil;
import com.aicompanion.mapper.AdminMapper;
import com.aicompanion.mapper.UserMapper;
import com.aicompanion.model.entity.Admin;
import com.aicompanion.model.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：校验 Token 有效性，支持 user/admin 双表认证
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final AdminMapper adminMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String token = SecurityUtil.extractToken(request);

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

        // 根据 userType 验证用户是否存在且状态正常
        if ("ADMIN".equals(userType)) {
            Admin admin = adminMapper.selectById(userId);
            if (admin == null || admin.getStatus() != 1) {
                writeError(response, 401, "账号不存在或已被禁用");
                return false;
            }
        } else {
            User user = userMapper.selectById(userId);
            if (user == null || user.getStatus() != 1) {
                writeError(response, 401, "账号不存在或已被禁用");
                return false;
            }
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
