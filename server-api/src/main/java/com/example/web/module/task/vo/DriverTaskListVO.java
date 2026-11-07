package com.example.web.module.task.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 司机任务列表概要，按待执行、运输中、已完成与已终止分页展示。
 */
@Data
public class DriverTaskListVO {

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
     * 承载货物
     */
    private List<TaskGoodsItemVO> goods;

    /**
     * 装货省
     */
    private String pickupProvince;

    /**
     * 装货市
     */
    private String pickupCity;

    /**
     * 卸货省
     */
    private String deliveryProvince;

    /**
     * 卸货市
     */
    private String deliveryCity;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

}
