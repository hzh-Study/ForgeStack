package com.example.web.module.reimbursement.vo;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.Data;

/**
 * 报销单详情，包含当前版本、凭证照片、全部历史版本与审核记录。
 */
@Data
public class ReimbursementDetailVO {

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
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 司机姓名
     */
    private String driverName;

    /**
     * 物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 物流公司名称
     */
    private String logisticsName;

    /**
     * 当前展示版本号
     */
    private Integer versionNo;

    /**
     * 当前展示版本是否为最新版本：1 是，0 否
     */
    private Integer isCurrent;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 审核状态名称
     */
    private String auditStatusName;

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
     * 审核人姓名
     */
    private String reviewerName;

    /**
     * 审核时间
     */
    private LocalDateTime reviewTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

    /**
     * 当前版本凭证照片
     */
    private List<ReimbursementPhotoVO> photos;

    /**
     * 全部历史版本
     */
    private List<ReimbursementVersionVO> versions;

    /**
     * 全部审核记录
     */
    private List<ReimbursementReviewVO> reviews;

}
