package com.example.web.module.order.vo;

import lombok.Data;

/**
 * 订单池公开货物概要，只展示接单判断需要的名称、类型、数量与单位。
 */
@Data
public class OrderPoolGoodsVO {

    /**
     * 货物名称
     */
    private String goodsName;

    /**
     * 货物类型编码
     */
    private String goodsType;

    /**
     * 货物数量
     */
    private Double quantity;

    /**
     * 计量单位
     */
    private String unit;

}
