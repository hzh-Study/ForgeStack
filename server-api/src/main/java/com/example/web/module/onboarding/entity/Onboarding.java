package com.example.web.module.onboarding.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 货主与物流公司企业入驻申请实体。
 */
@Data
@TableName("biz_onboarding")
public class Onboarding {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 申请账号 ID
     */
    private Long accountId;

    /**
     * 申请角色：SHIPPER 货主，LOGISTICS 物流公司
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
     * 统一社会信用代码模拟校验状态
     */
    private String creditVerifyStatus;

    /**
     * 法定代表人姓名
     */
    private String legalRepName;

    /**
     * 法定代表人证件类型
     */
    private String legalRepIdType;

    /**
     * 法定代表人证件号码
     */
    private String legalRepIdNo;

    /**
     * 法定代表人实名记录 ID
     */
    private Long legalRepRealnameId;

    /**
     * 联系人姓名
     */
    private String contactName;

    /**
     * 联系人电话
     */
    private String contactPhone;

    /**
     * 省
     */
    private String province;

    /**
     * 市
     */
    private String city;

    /**
     * 区
     */
    private String district;

    /**
     * 经营范围
     */
    private String businessScope;

    /**
     * 审核状态：PENDING 待审核，APPROVED 已通过，REJECTED 已驳回
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

