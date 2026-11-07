package com.example.web.module.vehicle.vo;

import java.time.LocalDate;
import lombok.Data;

/**
 * 司机当前绑定车辆详情。
 */
@Data
public class DriverVehicleVO {

    /**
     * 车辆 ID
     */
    private Long vehicleId;

    /**
     * 车牌号
     */
    private String plateNo;

    /**
     * 车辆类型编码
     */
    private String vehicleType;

    /**
     * 行驶证号
     */
    private String licenseNo;

    /**
     * 行驶证有效期
     */
    private LocalDate licenseExpireDate;

    /**
     * 车辆状态
     */
    private String vehicleStatus;

    /**
     * 资质附件相对路径，多个以英文逗号分隔
     */
    private String attachmentPaths;

}
