package com.example.web.module.order.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 货运订单实体，接单后继续承载运单身份。
 */
@Data
@TableName("biz_order")
public class Order {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 货主账号 ID
     */
    private Long shipperAccountId;

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
     * 备注
     */
    private String remark;

    /**
     * 接单物流公司账号 ID
     */
    private Long logisticsAccountId;

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

}

