package com.example.web.module.onboarding.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 入驻申请驳回参数。
 */
@Data
public class OnboardingRejectDTO {

    /**
     * 驳回原因
     */
    @NotBlank(message = "驳回原因不能为空")
    private String reason;

}
