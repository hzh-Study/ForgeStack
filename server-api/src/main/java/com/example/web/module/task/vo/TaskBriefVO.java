package com.example.web.module.task.vo;

import lombok.Data;

/**
 * 任务操作结果概要，指派、开放自抢、领取、弃单、回收与货物调整统一返回。
 */
@Data
public class TaskBriefVO {

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
     * 归属方式
     */
    private String assignType;

    /**
     * 是否开放自抢：1 是，0 否
     */
    private Integer openClaim;

    /**
     * 司机是否已知悉：1 是，0 否
     */
    private Integer driverAck;

}
