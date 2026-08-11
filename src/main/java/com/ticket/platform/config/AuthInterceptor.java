package com.ticket.platform.config;

import com.ticket.platform.common.BizException;
import com.ticket.platform.entity.User;
import com.ticket.platform.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * 登录拦截器：
 *  - 解析 Authorization: Bearer <token>，还原当前用户，放进 request attribute「loginUser」
 *  - /api/admin/** 必须是 ADMIN 角色（root），否则无权限
 * 抛出的 BizException 由 GlobalExceptionHandler 统一转成友好提示。
 */
@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    public static final String LOGIN_USER_ATTR = "loginUser";

    private final AuthService authService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        // 跨域预检请求不带 token，直接放行，由 CorsConfig 处理
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            return true;
        }
        String token = AuthService.resolveToken(request);
        User user = authService.getUserByToken(token);
        if (user == null) {
            throw new BizException("未登录或登录已过期");
        }

        // 管理端接口：仅 ADMIN 角色可访问
        if (request.getRequestURI().startsWith("/api/admin/") && !"ADMIN".equals(user.getRole())) {
            throw new BizException("无权限，仅管理员可操作");
        }

        request.setAttribute(LOGIN_USER_ATTR, user);
        return true;
    }
}
