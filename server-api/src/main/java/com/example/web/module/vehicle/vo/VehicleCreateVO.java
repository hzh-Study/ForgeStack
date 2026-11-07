package com.example.web.module.vehicle.vo;

import lombok.Data;

/**
 * 新增车辆结果。
 */
@Data
public class VehicleCreateVO {

    /**
     * 车辆 ID
     */
    private Long vehicleId;

    /**
     * 车牌号
     */
    private String plateNo;

    /**
     * 车辆状态
     */
    private String vehicleStatus;

}
