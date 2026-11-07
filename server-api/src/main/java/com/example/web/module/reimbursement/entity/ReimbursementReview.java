package com.example.web.module.reimbursement.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报销审核记录实体。
 */
@Data
@TableName("biz_reimbursement_review")
public class ReimbursementReview {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 报销单 ID
     */
    private Long reimbursementId;

    /**
     * 审核动作：SPECIAL_REVIEW 特殊复核，APPROVE 通过，REJECT 驳回
     */
    private String reviewAction;

    /**
     * 审核说明
     */
    private String note;

    /**
     * 操作人账号 ID
     */
    private Long operatorAccountId;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

