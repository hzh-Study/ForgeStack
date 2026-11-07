package com.example.web.module.task.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 物流公司运单列表概要，展示拆分与派车进度关键指标。
 */
@Data
public class WaybillListVO {

    /**
     * 订单（运单）ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 运费报价
     */
    private Double freightAmount;

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
     * 货物行数
     */
    private Integer goodsCount;

    /**
     * 已分配货物行数
     */
    private Integer assignedGoodsCount;

    /**
     * 未分配货物行数
     */
    private Integer unassignedGoodsCount;

    /**
     * 任务数
     */
    private Integer taskCount;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

}
