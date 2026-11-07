package com.example.web.module.order.dto;

import lombok.Data;

/**
 * 订单池分页查询参数，支持起止地、货物类型、运费与排序筛选。
 */
@Data
public class OrderPoolQueryDTO {

    /**
     * 装货省份
     */
    private String pickupProvince;

    /**
     * 装货城市
     */
    private String pickupCity;

    /**
     * 卸货省份
     */
    private String deliveryProvince;

    /**
     * 卸货城市
     */
    private String deliveryCity;

    /**
     * 货物类型编码
     */
    private String goodsType;

    /**
     * 运费下限
     */
    private Double minFreight;

    /**
     * 运费上限
     */
    private Double maxFreight;

    /**
     * 排序字段：publishTime、expireTime、freightAmount
     */
    private String sortBy = "publishTime";

    /**
     * 排序方向：asc 升序，desc 降序
     */
    private String sortOrder = "desc";

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
