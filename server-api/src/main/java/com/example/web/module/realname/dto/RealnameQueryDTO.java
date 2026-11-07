package com.example.web.module.realname.dto;

import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.realname.enums.VerifyStatusEnum;
import lombok.Data;

/**
 * 管理员实名列表查询参数。
 */
@Data
public class RealnameQueryDTO {

    /**
     * 角色筛选
     */
    private AccountRoleEnum role;

    /**
     * 校验状态筛选
     */
    private VerifyStatusEnum verifyStatus;

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
