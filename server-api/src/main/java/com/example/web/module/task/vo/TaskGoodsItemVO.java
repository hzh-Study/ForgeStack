package com.example.web.module.task.vo;

import lombok.Data;

/**
 * 任务承载货物展示信息，司机端与运单任务共用。
 */
@Data
public class TaskGoodsItemVO {

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
     * 数量
     */
    private Double quantity;

    /**
     * 重量（吨）
     */
    private Double weight;

    /**
     * 体积（立方米）
     */
    private Double volume;

    /**
     * 计量单位
     */
    private String unit;

}
