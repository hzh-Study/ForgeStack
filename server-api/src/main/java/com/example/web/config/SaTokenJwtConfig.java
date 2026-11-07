package com.example.web.config;

import cn.dev33.satoken.jwt.StpLogicJwtForSimple;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token JWT 配置。
 *
 * <p>使用 Simple 模式：Token 本身是 JWT，同时保留服务端会话，
 * 因此退出登录、踢人下线和禁用账号可以立即生效。</p>
 */
@Configuration
public class SaTokenJwtConfig {

    /**
     * 使用 Sa-Token JWT 简单模式颁发和解析 Token。
     *
     * @return Sa-Token 登录逻辑
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForSimple();
    }
}
