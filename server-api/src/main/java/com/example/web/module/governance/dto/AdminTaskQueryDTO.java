package com.example.web.module.governance.dto;

import lombok.Data;

/**
 * 管理员全平台任务分页查询参数。
 */
@Data
public class AdminTaskQueryDTO {

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 任务状态
     */
    private String taskStatus;

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
