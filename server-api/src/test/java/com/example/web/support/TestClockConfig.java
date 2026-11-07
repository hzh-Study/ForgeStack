package com.example.web.support;

import com.example.web.common.time.ClockService;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

/**
 * 测试时间配置，用可调时间服务替换系统时间服务。
 */
@TestConfiguration
public class TestClockConfig {

    /**
     * 注册可调时间服务并优先注入。
     *
     * @return 可调时间服务
     */
    @Bean
    @Primary
    public MutableClockService mutableClockService() {
        return new MutableClockService();
    }
}
