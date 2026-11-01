package com.example.web.module.system.vo;

import java.time.LocalDateTime;

/**
 * 服务健康检查信息。
 */
public class HealthVO {

    /**
     * 服务状态。
     */
    private String status;

    /**
     * 当前服务时间。
     */
    private LocalDateTime time;

    public HealthVO() {
    }

    public HealthVO(String status, LocalDateTime time) {
        this.status = status;
        this.time = time;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getTime() {
        return time;
    }

    public void setTime(LocalDateTime time) {
        this.time = time;
    }
}
