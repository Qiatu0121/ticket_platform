package com.ticket.platform.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * 注册登录拦截器：
 *  - 需要鉴权：/api/admin/**（仅 ADMIN）、/api/order/**（登录即可）、/api/user/logout
 *  - 放行：登录、注册、活动浏览（匿名可看）
 */
@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/admin/**", "/api/order/**", "/api/user/logout")
                .excludePathPatterns("/api/user/login", "/api/user/register");
    }
}
