package com.example.web.module.account.dto;

import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.module.account.enums.AccountRoleEnum;
import lombok.Data;

/**
 * 四类账号分页查询参数。
 */
@Data
public class AccountQueryDTO {

    /**
     * 角色筛选
     */
    private AccountRoleEnum role;

    /**
     * 状态筛选
     */
    private EnableStatusEnum status;

    /**
     * 手机号或展示名称关键词
     */
    private String keyword;

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
