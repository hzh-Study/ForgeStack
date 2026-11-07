package com.example.web.module.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * 车辆状态维护参数，仅允许 NORMAL、REPAIR、DISABLED。
 */
@Data
public class VehicleStatusDTO {

    /**
     * 目标车辆状态
     */
    @NotBlank(message = "车辆状态不能为空")
    private String status;

}
