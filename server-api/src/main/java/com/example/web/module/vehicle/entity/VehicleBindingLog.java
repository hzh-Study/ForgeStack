package com.example.web.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 司机车辆绑定调整留痕实体。
 */
@Data
@TableName("biz_vehicle_binding_log")
public class VehicleBindingLog {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 司机 ID
     */
    private Long driverId;

    /**
     * 调整前车辆 ID
     */
    private Long oldVehicleId;

    /**
     * 调整后车辆 ID
     */
    private Long newVehicleId;

    /**
     * 操作人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

