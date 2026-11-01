package com.example.web.module.user.dto;

import com.example.web.common.enums.EnableStatusEnum;
import jakarta.validation.constraints.NotBlank;

/**
 * 编辑前台用户参数。
 */
public class UserUpdateDTO {

    /**
     * 用户 ID。
     */
    private Long id;

    /**
     * 用户昵称。
     */
    @NotBlank(message = "昵称不能为空")
    private String nickname;

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

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
