package com.example.web.module.order.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 货主订单完整详情，接单后包含物流公司联系人与任务概要。
 */
@Data
public class ShipperOrderDetailVO {

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
     * 货主账号 ID
     */
    private Long shipperAccountId;

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
     * 发货备注
     */
    private String remark;

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
     * 接单物流公司主体名称
     */
    private String logisticsName;

    /**
     * 接单物流公司联系人
     */
    private String logisticsContactName;

    /**
     * 接单物流公司联系电话
     */
    private String logisticsContactPhone;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 接单时间
     */
    private LocalDateTime acceptTime;

    /**
     * 重发来源订单 ID
     */
    private Long sourceOrderId;

    /**
     * 重发次数
     */
    private Integer republishCount;

    /**
     * 撤回时间
     */
    private LocalDateTime withdrawTime;

    /**
     * 取消原因
     */
    private String cancelReason;

    /**
     * 取消操作账号 ID
     */
    private Long cancelByAccountId;

    /**
     * 取消时间
     */
    private LocalDateTime cancelTime;

    /**
     * 自动下架时间
     */
    private LocalDateTime offshelfTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 货物明细
     */
    private List<OrderGoodsVO> goods;

    /**
     * 装货点完整地址与联系人
     */
    private OrderAddressVO pickup;

    /**
     * 卸货点完整地址与联系人
     */
    private OrderAddressVO delivery;

    /**
     * 任务概要
     */
    private List<OrderTaskSummaryVO> tasks;

    /**
     * 到期提醒历史
     */
    private List<OrderReminderVO> reminders;

}
