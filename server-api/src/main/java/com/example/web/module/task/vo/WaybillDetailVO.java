package com.example.web.module.task.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 运单完整详情，含装卸货地址、货物分配状态、任务与进度。
 */
@Data
public class WaybillDetailVO {

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
     * 货主账号 ID，用于物流公司与货主发起会话
     */
    private Long shipperAccountId;

    /**
     * 货主名称
     */
    private String shipperName;

    /**
     * 运费报价
     */
    private Double freightAmount;

    /**
     * 发货备注
     */
    private String remark;

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
     * 装货点完整地址
     */
    private com.example.web.module.order.vo.OrderAddressVO pickup;

    /**
     * 卸货点完整地址
     */
    private com.example.web.module.order.vo.OrderAddressVO delivery;

    /**
     * 货物行与分配状态
     */
    private List<WaybillGoodsVO> goods;

    /**
     * 任务列表
     */
    private List<WaybillTaskVO> tasks;

    /**
     * 运单进度
     */
    private WaybillProgressVO progress;

}
