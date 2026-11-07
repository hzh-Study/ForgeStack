package com.example.web.module.account.vo;

import com.example.web.common.enums.EnableStatusEnum;
import java.time.LocalDateTime;
import lombok.Data;

/**
 * 四类账号列表展示信息，只返回治理需要的非敏感字段。
 */
@Data
public class AccountListVO {

    /**
     * 账号 ID
     */
    private Long accountId;

    /**
     * 角色编码
     */
    private String role;

    /**
     * 角色中文名称
     */
    private String roleName;

    /**
     * 登录手机号
     */
    private String username;

    /**
     * 账号状态
     */
    private EnableStatusEnum status;

    /**
     * 角色资料中的展示名称
     */
    private String displayName;

    /**
     * 账号创建时间
     */
    private LocalDateTime createTime;

}
