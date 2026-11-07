package com.example.web.module.checkin.vo;

import lombok.Data;

/**
 * 司机单任务打卡明细，包含出发、到达两个节点的全部版本与追加说明。
 */
@Data
public class TaskCheckinDetailVO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 出发打卡版本
     */
    private java.util.List<CheckinVersionVO> departVersions;

    /**
     * 到达打卡版本
     */
    private java.util.List<CheckinVersionVO> arriveVersions;

    /**
     * 追加说明
     */
    private java.util.List<CheckinRemarkVO> remarks;

}
