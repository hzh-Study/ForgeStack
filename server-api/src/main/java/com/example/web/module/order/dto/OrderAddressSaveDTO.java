package com.example.web.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 装卸货点保存参数，地址结构化到省市区加详细地址。
 */
@Data
public class OrderAddressSaveDTO {

    /**
     * 省
     */
    @NotBlank(message = "省份不能为空")
    private String province;

    /**
     * 市
     */
    @NotBlank(message = "城市不能为空")
    private String city;

    /**
     * 区
     */
    @NotBlank(message = "区县不能为空")
    private String district;

    /**
     * 详细地址，接单前不对外展示
     */
    @NotBlank(message = "详细地址不能为空")
    @Size(max = 300, message = "详细地址不能超过 300 个字符")
    private String detail;

    /**
     * 联系人姓名，接单前不对外展示
     */
    @NotBlank(message = "联系人姓名不能为空")
    @Size(max = 50, message = "联系人姓名不能超过 50 个字符")
    private String contactName;

    /**
     * 联系人电话，接单前不对外展示
     */
    @NotBlank(message = "联系人电话不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "联系人电话格式不正确")
    private String contactPhone;

}
