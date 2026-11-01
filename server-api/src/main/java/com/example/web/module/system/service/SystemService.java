package com.example.web.module.system.service;

import com.example.web.module.system.vo.HealthVO;
import com.example.web.module.system.vo.SystemInfoVO;

/**
 * 公共系统业务接口。
 */
public interface SystemService {

    /**
     * 查询服务健康状态。
     *
     * @return 服务健康状态
     */
    HealthVO getHealth();

    /**
     * 查询系统基础信息。
     *
     * @return 系统基础信息
     */
    SystemInfoVO getSystemInfo();
}
