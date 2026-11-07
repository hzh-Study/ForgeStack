package com.example.web.module.vehicle.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.vehicle.dto.VehicleCreateDTO;
import com.example.web.module.vehicle.dto.VehicleQueryDTO;
import com.example.web.module.vehicle.dto.VehicleStatusDTO;
import com.example.web.module.vehicle.vo.DriverVehicleVO;
import com.example.web.module.vehicle.vo.VehicleCreateVO;
import com.example.web.module.vehicle.vo.VehicleListVO;

/**
 * 物流公司车辆与司机绑定车辆业务接口。
 */
public interface VehicleService {

    /**
     * 分页查询本公司车辆，附带绑定司机姓名。
     *
     * @param dto 查询参数
     * @return 车辆分页数据
     */
    PageResult<VehicleListVO> pageVehicles(VehicleQueryDTO dto);

    /**
     * 新增本公司车辆，车牌全平台唯一并默认正常状态。
     *
     * @param dto 新增参数
     * @return 新增结果
     */
    VehicleCreateVO createVehicle(VehicleCreateDTO dto);

    /**
     * 维护本公司车辆状态，有运输中任务时禁止改为维修或停用。
     *
     * @param vehicleId 车辆 ID
     * @param dto 状态参数
     */
    void updateStatus(Long vehicleId, VehicleStatusDTO dto);

    /**
     * 查询当前司机绑定车辆，无绑定时返回空数据。
     *
     * @return 绑定车辆详情或 null
     */
    DriverVehicleVO getCurrentDriverVehicle();
}
