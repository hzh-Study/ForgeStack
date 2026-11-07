package com.example.web.module.task.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 任务指派参数，指定本公司司机及其绑定车辆。
 */
@Data
public class TaskDispatchDTO {

    /**
     * 司机资料 ID
     */
    @NotNull(message = "司机不能为空")
    private Long driverId;

    /**
     * 车辆 ID
     */
    @NotNull(message = "车辆不能为空")
    private Long vehicleId;

}
