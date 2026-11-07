package com.example.web.module.driver.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物流公司司机列表项，证件号统一脱敏。
 */
@Data
public class DriverListVO {

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
     * 脱敏后的身份证号
     */
    private String idNo;

    /**
     * 驾驶证号
     */
    private String licenseNo;

    /**
     * 驾驶证有效期
     */
    private LocalDate licenseExpireDate;

    /**
     * 绑定车辆 ID
     */
    private Long vehicleId;

    /**
     * 绑定车辆车牌号
     */
    private String vehiclePlateNo;

    /**
     * 在职状态
     */
    private String employmentStatus;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
