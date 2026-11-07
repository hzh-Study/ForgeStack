package com.example.web.module.vehicle.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.vehicle.dto.VehicleCreateDTO;
import com.example.web.module.vehicle.dto.VehicleQueryDTO;
import com.example.web.module.vehicle.dto.VehicleStatusDTO;
import com.example.web.module.vehicle.service.VehicleService;
import com.example.web.module.vehicle.vo.VehicleCreateVO;
import com.example.web.module.vehicle.vo.VehicleListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司车辆管理接口。
 */
@RestController
@RequestMapping("/api/logistics/vehicles")
public class ApiLogisticsVehicleController {

    private final VehicleService vehicleService;

    public ApiLogisticsVehicleController(VehicleService vehicleService) {
        this.vehicleService = vehicleService;
    }

    /**
     * 分页查询本公司车辆。
     *
     * @param dto 查询参数
     * @return 车辆分页数据
     */
    @GetMapping
    public Result<PageResult<VehicleListVO>> pageVehicles(VehicleQueryDTO dto) {
        return Result.success(vehicleService.pageVehicles(dto));
    }

    /**
     * 新增本公司车辆。
     *
     * @param dto 新增参数
     * @return 新增结果
     */
    @PostMapping
    public Result<VehicleCreateVO> createVehicle(@Valid @RequestBody VehicleCreateDTO dto) {
        return Result.success(vehicleService.createVehicle(dto));
    }

    /**
     * 维护本公司车辆状态。
     *
     * @param vehicleId 车辆 ID
     * @param dto 状态参数
     * @return 空结果
     */
    @PostMapping("/{vehicleId}/status")
    public Result<Void> updateStatus(@PathVariable Long vehicleId, @Valid @RequestBody VehicleStatusDTO dto) {
        vehicleService.updateStatus(vehicleId, dto);
        return Result.success();
    }
}
