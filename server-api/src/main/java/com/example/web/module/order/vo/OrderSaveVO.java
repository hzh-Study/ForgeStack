package com.example.web.module.order.vo;

import lombok.Data;

/**
 * 发货单保存结果，返回订单 ID 与当前状态。
 */
@Data
public class OrderSaveVO {

    /**
     * 订单 ID
     */
    private Long id;

    /**
     * 订单状态
     */
    private String orderStatus;

}
