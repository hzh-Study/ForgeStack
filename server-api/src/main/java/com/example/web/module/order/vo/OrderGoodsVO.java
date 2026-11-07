package com.example.web.module.order.vo;

import lombok.Data;

/**
 * 订单货物明细展示信息，供货主详情与运单详情使用。
 */
@Data
public class OrderGoodsVO {

    /**
     * 货物明细 ID
     */
    private Long id;

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
     * 货物重量（吨）
     */
    private Double weight;

    /**
     * 货物体积（立方米）
     */
    private Double volume;

    /**
     * 计量单位
     */
    private String unit;

    /**
     * 货物备注
     */
    private String remark;

}
