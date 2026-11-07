package com.example.web.module.order.vo;

import lombok.Data;

/**
 * 订单任务概要，展示任务号、状态、司机姓名与车牌。
 */
@Data
public class OrderTaskSummaryVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 任务状态
     */
    private String taskStatus;

    /**
     * 执行司机姓名
     */
    private String driverName;

    /**
     * 执行车辆车牌号
     */
    private String vehiclePlateNo;

}
