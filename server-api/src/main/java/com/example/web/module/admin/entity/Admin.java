package com.example.web.module.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;

/**
 * 管理员资料实体，通过 accountId 与统一账号表一一关联。
 */
@TableName("sys_admin")
public class Admin {

    /**
     * 管理员 ID，独立自增。
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 关联的统一账号 ID。
     */
    private Long accountId;

    /**
     * 管理员姓名。
     */
    private String realName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 管理员邮箱。
     */
    private String email;

    /**
     * 管理员头像相对地址。
     */
    private String avatar;

    /**
     * 创建时间。
     */
    private LocalDateTime createTime;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public String getRealName() {
        return realName;
    }

    public void setRealName(String realName) {
        this.realName = realName;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getAvatar() {
        return avatar;
    }

    public void setAvatar(String avatar) {
        this.avatar = avatar;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

}
