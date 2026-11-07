package com.example.web.module.driver.vo;

import lombok.Data;

/**
 * 创建司机结果。
 */
@Data
public class DriverCreateVO {

    /**
     * 司机资料 ID
     */
    private Long driverId;

    /**
     * 司机账号 ID
     */
    private Long accountId;

    /**
     * 司机姓名
     */
    private String name;

    /**
     * 司机手机号
     */
    private String phone;

    /**
     * 在职状态
     */
    private String employmentStatus;

}
