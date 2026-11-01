package com.example.web.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限拦截配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match(SaHttpMethod.OPTIONS).back();
            SaRouter.match("/admin/**")
                    .check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/auth/**")
                    .check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/files/**")
                    .check(r -> StpUtil.checkLogin());
        }).isAnnotation(true)).addPathPatterns("/**")
                .excludePathPatterns("/public/**", "/files/**");
    }
}
