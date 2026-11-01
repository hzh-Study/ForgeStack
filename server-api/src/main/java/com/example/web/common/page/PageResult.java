package com.example.web.common.page;

import java.util.List;

/**
 * 通用分页返回结构，后续业务列表接口可直接复用。
 *
 * @param <T> 列表数据类型
 */
public class PageResult<T> {

    /**
     * 当前页数据。
     */
    private List<T> records;

    /**
     * 总记录数。
     */
    private Long total;

    /**
     * 当前页码。
     */
    private Integer pageNum;

    /**
     * 每页条数。
     */
    private Integer pageSize;

    public PageResult() {
    }

    public PageResult(List<T> records, Long total, Integer pageNum, Integer pageSize) {
        this.records = records;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<T> getRecords() {
        return records;
    }

    public void setRecords(List<T> records) {
        this.records = records;
    }

    public Long getTotal() {
        return total;
    }

    public void setTotal(Long total) {
        this.total = total;
    }

    public Integer getPageNum() {
        return pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
