package com.example.web.module.system.service.impl;

import com.example.web.module.system.service.SystemService;
import com.example.web.module.system.vo.HealthVO;
import com.example.web.module.system.vo.SystemInfoVO;
import java.time.LocalDateTime;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 公共系统业务实现。
 */
@Service
public class SystemServiceImpl implements SystemService {

    /**
     * 系统名称，来源于唯一资源配置文件 application.yml。
     */
    @Value("${app.name}")
    private String appName;

    /**
     * 系统版本，来源于唯一资源配置文件 application.yml。
     */
    @Value("${app.version}")
    private String appVersion;

    /**
     * 查询服务健康状态，用于前后台验证后端服务是否可用。
     *
     * @return 服务健康状态
     */
    @Override
    public HealthVO getHealth() {
        return new HealthVO("UP", LocalDateTime.now());
    }

    /**
     * 查询系统基础信息，用于三端架构壳展示当前系统标识。
     *
     * @return 系统基础信息
     */
    @Override
    public SystemInfoVO getSystemInfo() {
        return new SystemInfoVO(appName, appVersion, "architecture-shell");
    }
}
