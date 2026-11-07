package com.example.web.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报销单实体，版本化保存提交与审核历史。
 */
@Data
@TableName("biz_reimbursement")
public class Reimbursement {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 运输任务 ID
     */
    private Long taskId;

    /**
     * 订单 ID
     */
    private Long orderId;

    /**
     * 司机账号 ID
     */
    private Long driverAccountId;

    /**
     * 所属物流公司账号 ID
     */
    private Long logisticsAccountId;

    /**
     * 版本号
     */
    private Integer versionNo;

    /**
     * 是否当前版本：1 是，0 否
     */
    private Integer isCurrent;

    /**
     * 审核状态：PENDING 待审核，APPROVED 已通过，REJECTED 已驳回
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
     * 审核驳回原因
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

}

