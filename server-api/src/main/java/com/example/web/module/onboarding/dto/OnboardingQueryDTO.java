package com.example.web.module.onboarding.dto;

import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.onboarding.enums.AuditStatusEnum;
import lombok.Data;

/**
 * 管理员入驻申请分页查询参数。
 */
@Data
public class OnboardingQueryDTO {

    /**
     * 审核状态筛选
     */
    private AuditStatusEnum auditStatus;

    /**
     * 申请角色筛选
     */
    private AccountRoleEnum role;

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
