package com.example.web.module.admin.dto;

import com.example.web.common.enums.EnableStatusEnum;
import jakarta.validation.constraints.NotBlank;

/**
 * 编辑管理员参数。
 */
public class AdminUpdateDTO {

    /**
     * 管理员 ID。
     */
    private Long id;

    /**
     * 管理员姓名。
     */
    @NotBlank(message = "姓名不能为空")
    private String realName;

    /**
     * 手机号。
     */
    private String phone;

    /**
     * 邮箱。
     */
    private String email;

    /**
     * 头像相对地址。
     */
    private String avatar;

    /**
     * 账号状态。
     */
    private EnableStatusEnum status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public EnableStatusEnum getStatus() {
        return status;
    }

    public void setStatus(EnableStatusEnum status) {
        this.status = status;
    }
}
