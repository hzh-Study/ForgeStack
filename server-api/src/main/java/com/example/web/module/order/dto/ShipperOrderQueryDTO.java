package com.example.web.module.order.dto;

import lombok.Data;

/**
 * 货主我的订单分页查询参数，时间范围按发布时间过滤。
 */
@Data
public class ShipperOrderQueryDTO {

    /**
     * 订单状态筛选
     */
    private String orderStatus;

    /**
     * 发布时间起，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
     */
    private String startTime;

    /**
     * 发布时间止，格式 yyyy-MM-dd HH:mm:ss 或 yyyy-MM-dd
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
