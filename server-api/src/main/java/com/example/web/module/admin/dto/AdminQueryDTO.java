package com.example.web.module.admin.dto;

import com.example.web.common.enums.EnableStatusEnum;

/**
 * 管理员分页查询参数。
 */
public class AdminQueryDTO {

    /**
     * 账号或姓名关键词。
     */
    private String keyword;

    /**
     * 账号状态。
     */
    private EnableStatusEnum status;

    /**
     * 当前页码。
     */
    private Integer pageNum = 1;

    /**
     * 每页条数。
     */
    private Integer pageSize = 10;

    public String getKeyword() {
        return keyword;
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }

    public Integer getPageNum() {
        return pageNum == null || pageNum < 1 ? 1 : pageNum;
    }

    public void setPageNum(Integer pageNum) {
        this.pageNum = pageNum;
    }

    public Integer getPageSize() {
        return pageSize == null || pageSize < 1 ? 10 : pageSize;
    }

    public void setPageSize(Integer pageSize) {
        this.pageSize = pageSize;
    }
}
