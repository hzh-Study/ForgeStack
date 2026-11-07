package com.example.web.module.reimbursement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 物流公司按司机覆盖月度报销额度参数。
 */
@Data
public class DriverQuotaDTO {

    /**
     * 司机账号 ID
     */
    @NotNull(message = "司机不能为空")
    private Long driverAccountId;

    /**
     * 年月，格式 yyyy-MM
     */
    @NotBlank(message = "年月不能为空")
    private String yearMonth;

    /**
     * 月度额度
     */
    @NotNull(message = "额度不能为空")
    private Double amount;

}
