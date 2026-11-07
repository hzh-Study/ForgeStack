package com.example.web.module.realname.vo;

import java.time.LocalDateTime;
import lombok.Data;

/**
 * 管理员实名列表展示信息，证件号统一脱敏。
 */
@Data
public class RealnameListVO {

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
     * 真实姓名
     */
    private String realName;

    /**
     * 证件类型
     */
    private String idType;

    /**
     * 脱敏后的证件号码
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
