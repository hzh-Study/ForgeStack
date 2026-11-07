package com.example.web.module.vehicle.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物流公司名下车辆实体，与司机一对一绑定。
 */
@Data
@TableName("biz_vehicle")
public class Vehicle {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 所属物流公司 ID
     */
    private Long logisticsId;

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
     * 车辆状态：NORMAL 正常，REPAIR 维修，DISABLED 停用
     */
    private String vehicleStatus;

    /**
     * 资质附件相对路径，多个以英文逗号分隔
     */
    private String attachmentPaths;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

