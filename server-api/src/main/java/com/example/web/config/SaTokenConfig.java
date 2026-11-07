package com.example.web.config;

import cn.dev33.satoken.interceptor.SaInterceptor;
import cn.dev33.satoken.router.SaHttpMethod;
import cn.dev33.satoken.router.SaRouter;
import cn.dev33.satoken.stp.StpUtil;
import com.example.web.security.BusinessAccessGuard;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Sa-Token 权限拦截配置。
 */
@Configuration
public class SaTokenConfig implements WebMvcConfigurer {

    private final BusinessAccessGuard businessAccessGuard;

    public SaTokenConfig(BusinessAccessGuard businessAccessGuard) {
        this.businessAccessGuard = businessAccessGuard;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(new SaInterceptor(handle -> {
            SaRouter.match(SaHttpMethod.OPTIONS).back();
            SaRouter.match("/admin/**").check(r -> StpUtil.checkLogin());
            SaRouter.match("/api/**").check(r -> StpUtil.checkLogin());
            SaRouter.match("/admin/**").check(r -> StpUtil.checkRole(com.example.web.security.RoleCode.ADMIN));
            SaRouter.match("/api/shipper/**").check(r -> StpUtil.checkRole(com.example.web.security.RoleCode.SHIPPER));
            SaRouter.match("/api/logistics/**").check(r -> StpUtil.checkRole(com.example.web.security.RoleCode.LOGISTICS));
            SaRouter.match("/api/driver/**").check(r -> StpUtil.checkRole(com.example.web.security.RoleCode.DRIVER));
            SaRouter.match("/admin/**").check(r -> businessAccessGuard.check());
            SaRouter.match("/api/**").notMatch("/api/auth/**").check(r -> businessAccessGuard.check());
        }).isAnnotation(true)).addPathPatterns("/**")
                .excludePathPatterns("/public/**");
    }
}
