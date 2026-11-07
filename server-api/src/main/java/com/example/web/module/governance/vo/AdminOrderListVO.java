package com.example.web.module.governance.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员全平台订单列表项。
 */
@Data
public class AdminOrderListVO {

    /**
     * 订单 ID
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
     * 货主账号 ID
     */
    private Long shipperAccountId;

    /**
     * 货主名称
     */
    private String shipperName;

    /**
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 物流公司名称
     */
    private String logisticsName;

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
     * 任务数
     */
    private Integer taskCount;

    /**
     * 发布时间
     */
    private LocalDateTime publishTime;

    /**
     * 到期时间
     */
    private LocalDateTime expireTime;

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

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
