package com.example.web.module.account.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.module.account.enums.AccountRoleEnum;
import java.time.LocalDateTime;

/**
 * 统一账号实体，是登录凭据和账号状态的唯一数据来源。
 */
@TableName("sys_account")
public class Account {

    /**
     * 账号 ID，由管理员表或用户表的 accountId 外键关联。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 账号角色：ADMIN 管理员，SHIPPER 货主，LOGISTICS 物流公司，DRIVER 司机。
     */
    private AccountRoleEnum role;

    /**
     * 登录账号，统一使用手机号，全角色范围内唯一。
     */
    private String username;

    /**
     * 密码摘要。
     */
    private String passwordHash;

    /**
     * 密码盐值。
     */
    private String passwordSalt;

    /**
     * 账号状态。
     */
    private EnableStatusEnum status;

    /**
     * 是否必须修改密码：1 是，0 否；司机使用初始密码首次登录后必须改密。
     */
    private Integer mustChangePassword;

    /**
     * 最后登录时间。
     */
    private LocalDateTime lastLoginTime;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public AccountRoleEnum getRole() { return role; }
    public void setRole(AccountRoleEnum role) { this.role = role; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPasswordHash() { return passwordHash; }
    public void setPasswordHash(String passwordHash) { this.passwordHash = passwordHash; }
    public String getPasswordSalt() { return passwordSalt; }
    public void setPasswordSalt(String passwordSalt) { this.passwordSalt = passwordSalt; }
    public EnableStatusEnum getStatus() { return status; }
    public void setStatus(EnableStatusEnum status) { this.status = status; }
    public Integer getMustChangePassword() { return mustChangePassword; }
    public void setMustChangePassword(Integer mustChangePassword) { this.mustChangePassword = mustChangePassword; }
    public LocalDateTime getLastLoginTime() { return lastLoginTime; }
    public void setLastLoginTime(LocalDateTime lastLoginTime) { this.lastLoginTime = lastLoginTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
