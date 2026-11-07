package com.example.web.module.order.vo;

import lombok.Data;

/**
 * 装卸货点完整展示信息，仅对货主本人与接单后相关方开放。
 */
@Data
public class OrderAddressVO {

    /**
     * 地址类型：PICKUP 装货点，DELIVERY 卸货点
     */
    private String addressType;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 详细地址
     */
    private String detail;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String contactPhone;

}
