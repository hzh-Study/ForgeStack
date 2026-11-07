package com.example.web.module.vehicle.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;
import lombok.Data;

/**
 * 物流公司新增车辆参数。
 */
@Data
public class VehicleCreateDTO {

    /**
     * 车牌号，全平台唯一
     */
    @NotBlank(message = "车牌号不能为空")
    private String plateNo;

    /**
     * 车辆类型编码
     */
    private String vehicleType;

    /**
     * 行驶证号
     */
    @NotBlank(message = "行驶证号不能为空")
    private String licenseNo;

    /**
     * 行驶证有效期
     */
    @NotNull(message = "行驶证有效期不能为空")
    private LocalDate licenseExpireDate;

    /**
     * 资质附件相对路径列表
     */
    private List<String> attachmentPaths;

}
