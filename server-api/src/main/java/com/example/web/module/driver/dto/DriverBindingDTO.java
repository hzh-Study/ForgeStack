package com.example.web.module.driver.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 调整司机绑定车辆参数。
 */
@Data
public class DriverBindingDTO {

    /**
     * 新绑定车辆 ID
     */
    @NotNull(message = "车辆不能为空")
    private Long vehicleId;

}
