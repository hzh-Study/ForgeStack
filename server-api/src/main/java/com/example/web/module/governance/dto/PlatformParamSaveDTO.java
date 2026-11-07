package com.example.web.module.governance.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 平台参数新增或修改参数。
 */
@Data
public class PlatformParamSaveDTO {

    /**
     * 参数键
     */
    @NotBlank(message = "参数键不能为空")
    @Size(max = 60, message = "参数键不能超过60个字符")
    private String paramKey;

    /**
     * 参数值
     */
    @NotBlank(message = "参数值不能为空")
    @Size(max = 200, message = "参数值不能超过200个字符")
    private String paramValue;

    /**
     * 参数说明
     */
    @Size(max = 200, message = "参数说明不能超过200个字符")
    private String description;

}
