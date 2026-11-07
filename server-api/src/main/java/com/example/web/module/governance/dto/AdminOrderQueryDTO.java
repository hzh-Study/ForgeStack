package com.example.web.module.governance.dto;

import lombok.Data;

/**
 * 管理员全平台订单分页查询参数。
 */
@Data
public class AdminOrderQueryDTO {

    /**
     * 货主账号 ID
     */
    private Long shipperAccountId;

    /**
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 订单状态
     */
    private String orderStatus;

    /**
     * 创建时间起，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private String startTime;

    /**
     * 创建时间止，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private String endTime;

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
