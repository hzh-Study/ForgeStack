package com.example.web.module.reimbursement.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 报销审核记录。
 */
@Data
public class ReimbursementReviewVO {

    /**
     * 审核记录 ID
     */
    private Long id;

    /**
     * 审核动作
     */
    private String action;

    /**
     * 审核动作名称
     */
    private String actionName;

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
