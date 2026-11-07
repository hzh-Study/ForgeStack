package com.example.web.module.driver.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.driver.dto.DriverBindingDTO;
import com.example.web.module.driver.dto.DriverCreateDTO;
import com.example.web.module.driver.dto.DriverQueryDTO;
import com.example.web.module.driver.service.DriverService;
import com.example.web.module.driver.vo.DriverCreateVO;
import com.example.web.module.driver.vo.DriverListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司司机管理接口。
 */
@RestController
@RequestMapping("/api/logistics/drivers")
public class ApiLogisticsDriverController {

    private final DriverService driverService;

    public ApiLogisticsDriverController(DriverService driverService) {
        this.driverService = driverService;
    }

    /**
     * 创建本公司司机账号与司机资料。
     *
     * @param dto 创建参数
     * @return 创建结果
     */
    @PostMapping
    public Result<DriverCreateVO> createDriver(@Valid @RequestBody DriverCreateDTO dto) {
        return Result.success(driverService.createDriver(dto));
    }

    /**
     * 分页查询本公司司机。
     *
     * @param dto 查询参数
     * @return 司机分页数据
     */
    @GetMapping
    public Result<PageResult<DriverListVO>> pageDrivers(DriverQueryDTO dto) {
        return Result.success(driverService.pageDrivers(dto));
    }

    /**
     * 调整本公司司机绑定车辆。
     *
     * @param driverId 司机资料 ID
     * @param dto 绑定参数
     * @return 空结果
     */
    @PostMapping("/{driverId}/binding")
    public Result<Void> adjustBinding(@PathVariable Long driverId, @Valid @RequestBody DriverBindingDTO dto) {
        driverService.adjustBinding(driverId, dto);
        return Result.success();
    }
}
