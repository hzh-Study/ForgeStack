package com.example.web.module.realname.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 本人实名详情，本人可见完整证件号。
 */
@Data
public class RealnameDetailVO {

    /**
     * 实名记录 ID
     */
    private Long id;

    /**
     * 绑定账号 ID
     */
    private Long accountId;

    /**
     * 绑定角色编码
     */
    private String role;

    /**
     * 实名类型
     */
    private String realnameType;

    /**
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
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
     * 校验状态
     */
    private String verifyStatus;

    /**
     * 不通过原因
     */
    private String failReason;

    /**
     * 校验时间
     */
    private LocalDateTime verifyTime;

    /**
     * 创建时间
     */
    private LocalDateTime createTime;

}
