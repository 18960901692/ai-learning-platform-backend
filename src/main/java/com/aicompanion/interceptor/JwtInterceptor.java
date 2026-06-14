package com.aicompanion.interceptor;

import com.aicompanion.common.response.Result;
import com.aicompanion.common.util.JwtUtil;
import com.aicompanion.common.util.SecurityUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * JWT 拦截器：校验 Token 有效性，并将用户信息存入 request attribute
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtInterceptor implements HandlerInterceptor {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

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
        String role = jwtUtil.getRole(token);
        SecurityUtil.setCurrentUser(request, userId, role);

        return true;
    }

    private void writeError(HttpServletResponse response, int code, String message) throws Exception {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(200);
        Result<Void> result = Result.fail(code, message);
        response.getWriter().write(objectMapper.writeValueAsString(result));
    }
}
