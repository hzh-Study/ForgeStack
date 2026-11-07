package com.example.web.module.order.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 订单取消参数，取消必须填写原因。
 */
@Data
public class OrderCancelDTO {

    /**
     * 取消原因
     */
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 300, message = "取消原因不能超过 300 个字符")
    private String reason;

}
