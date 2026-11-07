package com.example.web.module.task.vo;

import lombok.Data;

/**
 * 运单货物行展示信息，标记是否已分配到任务。
 */
@Data
public class WaybillGoodsVO {

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

    /**
     * 货物备注
     */
    private String remark;

    /**
     * 是否已分配到任务
     */
    private Boolean assigned;

    /**
     * 所属任务 ID
     */
    private Long taskId;

    /**
     * 所属任务号
     */
    private String taskNo;

}
