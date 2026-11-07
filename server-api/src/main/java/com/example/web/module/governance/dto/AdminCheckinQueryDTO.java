package com.example.web.module.governance.dto;

import lombok.Data;

/**
 * 管理员全平台打卡分页查询参数。
 */
@Data
public class AdminCheckinQueryDTO {

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 打卡节点：DEPART 出发，ARRIVE 到达
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
