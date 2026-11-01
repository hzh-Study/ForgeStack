package com.example.web.module.system.controller;

import com.example.web.common.result.Result;
import com.example.web.module.system.service.SystemService;
import com.example.web.module.system.vo.HealthVO;
import com.example.web.module.system.vo.SystemInfoVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公共系统接口，提供三端联通所需的无业务基础信息。
 */
@RestController
@RequestMapping("/public")
public class PublicSystemController {

    private final SystemService systemService;

    public PublicSystemController(SystemService systemService) {
        this.systemService = systemService;
    }

    /**
     * 健康检查接口，用于验证后端服务是否启动成功。
     *
     * @return 服务健康状态
     */
    @GetMapping("/health")
    public Result<HealthVO> health() {
        return Result.success(systemService.getHealth());
    }

    /**
     * 系统基础信息接口，用于前后台展示架构壳状态。
     *
     * @return 系统基础信息
     */
    @GetMapping("/system-info")
    public Result<SystemInfoVO> systemInfo() {
        return Result.success(systemService.getSystemInfo());
    }
}
