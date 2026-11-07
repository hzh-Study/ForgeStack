package com.example.web.module.vehicle.controller;

import com.example.web.common.result.Result;
import com.example.web.module.vehicle.service.VehicleService;
import com.example.web.module.vehicle.vo.DriverVehicleVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机当前绑定车辆接口。
 */
@RestController
@RequestMapping("/api/driver/vehicle")
public class ApiDriverVehicleController {

    private final VehicleService vehicleService;

    public ApiDriverVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * 查询当前司机绑定车辆，无绑定时返回空数据。
     *
     * @return 绑定车辆详情
     */
    @GetMapping
    public Result<DriverVehicleVO> getCurrentVehicle() {
        return Result.success(vehicleService.getCurrentDriverVehicle());
    }
}
