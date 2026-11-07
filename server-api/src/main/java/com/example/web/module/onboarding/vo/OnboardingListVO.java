package com.example.web.module.onboarding.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员入驻申请列表展示信息，证件号统一脱敏。
 */
@Data
public class OnboardingListVO {

    /**
     * 入驻申请 ID
     */
    private Long id;

    /**
     * 申请账号 ID
     */
    private Long accountId;

    /**
     * 申请账号手机号
     */
    private String accountUsername;

    /**
     * 申请角色编码
     */
    private String role;

    /**
     * 企业主体名称
     */
    private String subjectName;

    /**
     * 统一社会信用代码
     */
    private String creditCode;

    /**
     * 法定代表人姓名
     */
    private String legalRepName;

    /**
     * 脱敏后的法定代表人证件号
     */
    private String legalRepIdNo;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String contactPhone;

    /**
     * 审核状态
     */
    private String auditStatus;

    /**
     * 驳回原因
     */
    private String rejectReason;

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
