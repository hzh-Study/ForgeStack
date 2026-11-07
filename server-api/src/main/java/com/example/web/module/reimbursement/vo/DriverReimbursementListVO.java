package com.example.web.module.reimbursement.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 司机我的报销列表项。
 */
@Data
public class DriverReimbursementListVO {

    /**
     * 报销单 ID
     */
    private Long id;

    /**
     * 任务 ID
     */
    private Long taskId;

    /**
     * 任务号
     */
    private String taskNo;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 订单号
     */
    private String orderNo;

    /**
     * 运单号
     */
    private String waybillNo;

    /**
     * 报销金额
     */
    private Double amount;

    /**
     * 费用类型编码
     */
    private String expenseType;

    /**
     * 费用类型名称
     */
    private String expenseTypeName;

    /**
     * 费用发生日期
     */
    private LocalDate occurredDate;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 审核状态名称
     */
    private String auditStatusName;

    /**
     * 是否超限：1 是，0 否
     */
    private Integer overspend;

    /**
     * 驳回原因
     */
    private String reviewReason;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
