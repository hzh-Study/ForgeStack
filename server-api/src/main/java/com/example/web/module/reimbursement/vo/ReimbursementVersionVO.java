package com.example.web.module.reimbursement.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 报销单历史版本，保留每次提交的金额、凭证与审核结论。
 */
@Data
public class ReimbursementVersionVO {

    /**
     * 报销单 ID
     */
    private Long id;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 是否当前版本：1 是，0 否
     */
    private Integer isCurrent;

    /**
     * 审核状态
     */
    private String auditStatus;

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
     * 费用说明
     */
    private String description;

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

    /**
     * 特殊审核说明
     */
    private String specialReviewNote;

    /**
     * 驳回原因
     */
    private String reviewReason;

    /**
     * 审核人账号 ID
     */
    private Long reviewerAccountId;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 凭证照片
     */
    private List<ReimbursementPhotoVO> photos;

}
