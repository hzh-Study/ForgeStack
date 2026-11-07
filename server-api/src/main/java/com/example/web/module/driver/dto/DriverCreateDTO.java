package com.example.web.module.driver.dto;

import com.example.web.module.realname.enums.IdTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.time.LocalDate;
import lombok.Data;

/**
 * 物流公司创建司机参数，账号使用初始密码并强制首次改密。
 */
@Data
public class DriverCreateDTO {

    /**
     * 司机姓名
     */
    @NotBlank(message = "司机姓名不能为空")
    private String name;

    /**
     * 司机手机号，同时作为登录账号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    /**
     * 初始密码
     */
    @NotBlank(message = "初始密码不能为空")
    private String password;

    /**
     * 实名证件类型
     */
    @NotNull(message = "证件类型不能为空")
    private IdTypeEnum idType;

    /**
     * 实名证件号码
     */
    @NotBlank(message = "证件号码不能为空")
    private String idNo;

    /**
     * 驾驶证号
     */
    @NotBlank(message = "驾驶证号不能为空")
    private String licenseNo;

    /**
     * 驾驶证有效期
     */
    @NotNull(message = "驾驶证有效期不能为空")
    private LocalDate licenseExpireDate;

    /**
     * 邮箱
     */
    private String email;

    /**
     * 绑定车辆 ID
     */
    @NotNull(message = "绑定车辆不能为空")
    private Long vehicleId;

}
