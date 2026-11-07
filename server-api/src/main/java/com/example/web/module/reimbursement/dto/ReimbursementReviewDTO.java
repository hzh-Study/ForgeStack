package com.example.web.module.reimbursement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 物流公司报销审核参数。
 */
@Data
public class ReimbursementReviewDTO {

    /**
     * 审核动作：SPECIAL_REVIEW 特殊复核，APPROVE 通过，REJECT 驳回
     */
    @NotBlank(message = "审核动作不能为空")
    private String action;

    /**
     * 审核说明，特殊复核与驳回必填
     */
    @Size(max = 300, message = "审核说明不能超过300个字符")
    private String note;

}
