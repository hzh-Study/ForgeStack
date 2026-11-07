package com.example.web.module.reimbursement.vo;

import lombok.Data;

/**
 * 司机月度额度与使用情况。
 */
@Data
public class DriverQuotaVO {

    /**
     * 年月，格式 yyyy-MM
     */
    private String yearMonth;

    /**
     * 当月可用额度
     */
    private Double quota;

    /**
     * 当月已用额度，仅含待审核与已通过
     */
    private Double used;

    /**
     * 当月剩余额度
     */
    private Double remaining;

    /**
     * 平台默认额度
     */
    private Double defaultQuota;

}
