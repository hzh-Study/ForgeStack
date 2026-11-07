package com.example.web.module.task.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 公司任务池公开概要，领取前不展示详细地址、联系人与备注。
 */
@Data
public class DriverTaskPoolVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

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
     * 约定提货时间
     */
    private LocalDateTime pickupTime;

    /**
     * 约定送达时间
     */
    private LocalDateTime deliveryTime;

    /**
     * 所属物流公司名称
     */
    private String logisticsName;

}
