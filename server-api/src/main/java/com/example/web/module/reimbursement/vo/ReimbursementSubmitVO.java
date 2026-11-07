package com.example.web.module.reimbursement.vo;

import lombok.Data;

/**
 * 报销提交或重新提交结果，包含提交时的累计与额度快照。
 */
@Data
public class ReimbursementSubmitVO {

    /**
     * 报销单 ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 审核状态
     */
    private String status;

    /**
     * 审核状态，与 status 同值返回，便于前端统一使用 auditStatus 字段
     */
    private String auditStatus;

    /**
     * 报销金额
     */
    private Double amount;

    /**
     * 提交时当月累计金额快照
     */
    private Double totalSnapshot;

    /**
     * 提交时月度额度快照
     */
    private Double quotaSnapshot;

    /**
     * 是否超限：1 是，0 否
     */
    private Integer overspend;

}
