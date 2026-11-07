package com.example.web.module.order.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 货主我的订单列表展示信息，只返回订单概要。
 */
@Data
public class ShipperOrderListVO {

    /**
     * 订单 ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 运费报价
     */
    private Double freightAmount;

    /**
     * 有效期小时数
     */
    private Integer validHours;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 装货省
     */
    private String pickupProvince;

    /**
     * 装货市
     */
    private String pickupCity;

    /**
     * 装货区
     */
    private String pickupDistrict;

    /**
     * 卸货省
     */
    private String deliveryProvince;

    /**
     * 卸货市
     */
    private String deliveryCity;

    /**
     * 卸货区
     */
    private String deliveryDistrict;

    /**
     * 货物行数
     */
    private Integer goodsCount;

    /**
     * 货物名称概要
     */
    private String goodsSummary;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 接单物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 重发来源订单 ID
     */
    private Long sourceOrderId;

    /**
     * 重发次数
     */
    private Integer republishCount;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
