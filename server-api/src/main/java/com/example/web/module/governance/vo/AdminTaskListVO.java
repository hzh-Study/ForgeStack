package com.example.web.module.governance.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员全平台任务列表项。
 */
@Data
public class AdminTaskListVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 物流公司名称
     */
    private String logisticsName;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 司机手机号
     */
    private String driverPhone;

    /**
     * 车辆 ID
     */
    private Long vehicleId;

    /**
     * 车牌号
     */
    private String vehiclePlateNo;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 归属方式
     */
    private String assignType;

    /**
     * 是否开放自抢
     */
    private Integer openClaim;

    /**
     * 司机是否已知悉
     */
    private Integer driverAck;

    /**
     * 知悉确认时间
     */
    private LocalDateTime driverAckTime;

    /**
     * 是否已有出发打卡
     */
    private Boolean departChecked;

    /**
     * 是否已有到达打卡
     */
    private Boolean arriveChecked;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
