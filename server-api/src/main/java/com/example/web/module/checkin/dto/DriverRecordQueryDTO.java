package com.example.web.module.checkin.dto;

import lombok.Data;

/**
 * 司机 Web 端现场记录查询参数，时间范围按打卡提交时间过滤。
 */
@Data
public class DriverRecordQueryDTO {

    /**
     * 任务 ID，可为空
     */
    private Long taskId;

    /**
     * 提交时间起，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private String startTime;

    /**
     * 提交时间止，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private String endTime;

    /**
     * 打卡节点：DEPART 或 ARRIVE，可为空
     */
    private String node;

    /**
     * 当前页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 获取合法页码。
     *
     * @return 页码
     */
    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    /**
     * 获取合法每页条数。
     *
     * @return 每页条数
     */
    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
    }
}
