package com.example.web.module.driver.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.driver.dto.DriverBindingDTO;
import com.example.web.module.driver.dto.DriverCreateDTO;
import com.example.web.module.driver.dto.DriverQueryDTO;
import com.example.web.module.driver.vo.DriverCreateVO;
import com.example.web.module.driver.vo.DriverListVO;

/**
 * 物流公司司机管理业务接口。
 */
public interface DriverService {

    /**
     * 同一事务创建司机账号、实名记录与司机资料。
     *
     * @param dto 司机创建参数
     * @return 创建结果
     */
    DriverCreateVO createDriver(DriverCreateDTO dto);

    /**
     * 分页查询本公司司机，证件号脱敏。
     *
     * @param dto 查询参数
     * @return 司机分页数据
     */
    PageResult<DriverListVO> pageDrivers(DriverQueryDTO dto);

    /**
     * 调整本公司司机的绑定车辆，司机有进行中任务时拒绝。
     *
     * @param driverId 司机资料 ID
     * @param dto 绑定参数
     */
    void adjustBinding(Long driverId, DriverBindingDTO dto);
}
