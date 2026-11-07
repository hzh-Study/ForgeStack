package com.example.web.module.vehicle.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物流公司车辆列表项。
 */
@Data
public class VehicleListVO {

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
     * 绑定司机姓名
     */
    private String driverName;

    /**
     * 资质附件相对路径，多个以英文逗号分隔
     */
    private String attachmentPaths;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
