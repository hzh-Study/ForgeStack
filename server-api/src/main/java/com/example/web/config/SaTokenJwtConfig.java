package com.example.web.config;

import cn.dev33.satoken.jwt.StpLogicJwtForStateless;
import cn.dev33.satoken.stp.StpLogic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Sa-Token JWT 配置，让登录 Token 在后端重启后仍可解析。
 */
@Configuration
public class SaTokenJwtConfig {

    /**
     * 使用 Sa-Token JWT 无状态模式颁发和解析 Token。
     *
     * @return Sa-Token 登录逻辑
     */
    @Bean
    public StpLogic getStpLogicJwt() {
        return new StpLogicJwtForStateless();
    }
}
