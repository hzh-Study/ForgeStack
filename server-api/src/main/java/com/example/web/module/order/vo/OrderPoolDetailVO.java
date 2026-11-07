package com.example.web.module.order.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 订单池详情公开投影，与列表保持同一隐私边界，不返回货主联系方式与备注。
 */
@Data
public class OrderPoolDetailVO {

    /**
     * 订单 ID
     */
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

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
     * 货物概要
     */
    private List<OrderPoolGoodsVO> goods;

    /**
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 运费报价
     */
    private Double freightAmount;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

    /**
     * 剩余有效秒数
     */
    private Long remainingSeconds;

    /**
     * 剩余有效分钟数
     */
    private Long remainingMinutes;

    /**
     * 有效期小时数
     */
    private Integer validHours;

}
