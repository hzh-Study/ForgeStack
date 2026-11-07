package com.example.web.module.realname.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 实名身份实体，个人实名与企业法定代表人实名相互独立。
 */
@Data
@TableName("biz_realname")
public class Realname {

    /**
     * 主键
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 绑定账号 ID，法定代表人可不绑定
     */
    private Long accountId;

    /**
     * 绑定角色
     */
    private String role;

    /**
     * PERSONAL 个人实名，LEGAL_REP 法定代表人实名
     */
    private String realnameType;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型：ID_CARD 身份证，PASSPORT 护照
     */
    private String idType;

    /**
     * 证件号码
     */
    private String idNo;

    /**
     * 本人手机号
     */
    private String phone;

    /**
     * PENDING 待校验，PASSED 通过，FAILED 不通过
     */
    private String verifyStatus;

    /**
     * 不通过原因
     */
    private String failReason;

    /**
     * 关联企业入驻申请 ID
     */
    private Long enterpriseOnboardingId;

    /**
     * 校验时间
     */
    private LocalDateTime verifyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}

