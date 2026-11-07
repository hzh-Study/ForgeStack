package com.example.web.module.task.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 运输任务实体，是派车、打卡与报销的最小单位。
 */
@Data
@TableName("biz_task")
public class Task {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 所属订单（运单）ID
     */
    private Long orderId;

    /**
     * 所属物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 执行司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 执行司机资料 ID
     */
    private Long driverId;

    /**
     * 执行车辆 ID
     */
    private Long vehicleId;

    /**
     * 归属方式：ASSIGNED 指派，CLAIMED 自抢
     */
    private String assignType;

    /**
     * 是否开放自抢：1 是，0 否
     */
    private Integer openClaim;

    /**
     * 司机是否已知悉：1 是，0 否
     */
    private Integer driverAck;

    /**
     * 知悉确认时间
     */
    private LocalDateTime driverAckTime;

    /**
     * 运输中司机占用锁
     */
    private String driverLock;

    /**
     * 运输中车辆占用锁
     */
    private String vehicleLock;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

