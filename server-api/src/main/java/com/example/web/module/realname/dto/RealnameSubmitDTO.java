package com.example.web.module.realname.dto;

import com.example.web.module.realname.enums.IdTypeEnum;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

/**
 * 个人实名提交参数。
 */
@Data
public class RealnameSubmitDTO {

    /**
     * 真实姓名
     */
    @NotBlank(message = "真实姓名不能为空")
    private String realName;

    /**
     * 证件类型
     */
    @NotNull(message = "证件类型不能为空")
    private IdTypeEnum idType;

    /**
     * 证件号码
     */
    @NotBlank(message = "证件号码不能为空")
    private String idNo;

    /**
     * 本人手机号
     */
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

}
