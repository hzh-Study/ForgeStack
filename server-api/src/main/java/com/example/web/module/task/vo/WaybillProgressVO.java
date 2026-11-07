package com.example.web.module.task.vo;

import lombok.Data;

/**
 * 运单进度概要，供物流公司与货主判断执行情况。
 */
@Data
public class WaybillProgressVO {

    /**
     * 货物行总数
     */
    private Integer goodsTotal;

    /**
     * 已分配货物行数
     */
    private Integer assignedGoods;

    /**
     * 未分配货物行数
     */
    private Integer unassignedGoods;

    /**
     * 任务总数
     */
    private Integer taskTotal;

    /**
     * 待指派任务数
     */
    private Integer pendingAssignCount;

    /**
     * 待执行任务数
     */
    private Integer waitExecuteCount;

    /**
     * 运输中任务数
     */
    private Integer inTransportCount;

    /**
     * 已完成任务数
     */
    private Integer completedCount;

    /**
     * 已终止任务数
     */
    private Integer terminatedCount;

    /**
     * 已出发任务数
     */
    private Integer departCount;

    /**
     * 已到达任务数
     */
    private Integer arriveCount;

}
