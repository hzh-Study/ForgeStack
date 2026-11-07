package com.example.web.module.vehicle.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.governance.entity.DictItem;
import com.example.web.module.governance.enums.DictTypeEnum;
import com.example.web.module.governance.mapper.DictItemMapper;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.vehicle.dto.VehicleCreateDTO;
import com.example.web.module.vehicle.dto.VehicleQueryDTO;
import com.example.web.module.vehicle.dto.VehicleStatusDTO;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.enums.VehicleStatusEnum;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.module.vehicle.service.VehicleService;
import com.example.web.module.vehicle.vo.DriverVehicleVO;
import com.example.web.module.vehicle.vo.VehicleCreateVO;
import com.example.web.module.vehicle.vo.VehicleListVO;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 物流公司车辆与司机绑定车辆业务实现，车辆状态与运输中任务互斥校验。
 */
@Service
public class VehicleServiceImpl implements VehicleService {

    private final CurrentAccountService currentAccountService;
    private final VehicleMapper vehicleMapper;
    private final DriverMapper driverMapper;
    private final LogisticsMapper logisticsMapper;
    private final DictItemMapper dictItemMapper;
    private final TaskMapper taskMapper;
    private final ClockService clockService;

    public VehicleServiceImpl(CurrentAccountService currentAccountService, VehicleMapper vehicleMapper,
                              DriverMapper driverMapper, LogisticsMapper logisticsMapper,
                              DictItemMapper dictItemMapper, TaskMapper taskMapper, ClockService clockService) {
        this.currentAccountService = currentAccountService;
        this.vehicleMapper = vehicleMapper;
        this.driverMapper = driverMapper;
        this.logisticsMapper = logisticsMapper;
        this.dictItemMapper = dictItemMapper;
        this.taskMapper = taskMapper;
        this.clockService = clockService;
    }

    /**
     * 分页查询本公司车辆并补齐绑定司机姓名。
     *
     * @param dto 查询参数
     * @return 车辆分页数据
     */
    @Override
    public PageResult<VehicleListVO> pageVehicles(VehicleQueryDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        Page<Vehicle> page = vehicleMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()),
                new LambdaQueryWrapper<Vehicle>()
                        .eq(Vehicle::getLogisticsId, logistics.getId())
                        .orderByDesc(Vehicle::getId));
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
        }
        Map<Long, String> driverNameMap = loadDriverNames(logistics, page.getRecords());
        List<VehicleListVO> records = new ArrayList<>();
        for (Vehicle vehicle : page.getRecords()) {
            VehicleListVO vo = new VehicleListVO();
            vo.setVehicleId(vehicle.getId());
            vo.setPlateNo(vehicle.getPlateNo());
            vo.setVehicleType(vehicle.getVehicleType());
            vo.setLicenseNo(vehicle.getLicenseNo());
            vo.setLicenseExpireDate(vehicle.getLicenseExpireDate());
            vo.setVehicleStatus(vehicle.getVehicleStatus());
            vo.setDriverName(driverNameMap.get(vehicle.getId()));
            vo.setAttachmentPaths(vehicle.getAttachmentPaths());
            vo.setCreateTime(vehicle.getCreateTime());
            records.add(vo);
        }
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 新增本公司车辆，校验车牌全平台唯一与车辆类型字典。
     *
     * @param dto 新增参数
     * @return 新增结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public VehicleCreateVO createVehicle(VehicleCreateDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        String plateNo = dto.getPlateNo().trim();
        if (vehicleMapper.selectCount(new LambdaQueryWrapper<Vehicle>()
                .eq(Vehicle::getPlateNo, plateNo)) > 0) {
            throw new BusinessException("车牌号已存在");
        }
        String vehicleType = null;
        if (dto.getVehicleType() != null && !dto.getVehicleType().isBlank()) {
            vehicleType = dto.getVehicleType().trim();
            DictItem dictItem = dictItemMapper.selectOne(new LambdaQueryWrapper<DictItem>()
                    .eq(DictItem::getDictType, DictTypeEnum.VEHICLE_TYPE.getCode())
                    .eq(DictItem::getItemCode, vehicleType)
                    .eq(DictItem::getStatus, EnableStatusEnum.ENABLED.getCode()));
            if (dictItem == null) {
                throw new BusinessException("车辆类型不存在");
            }
        }

        Vehicle vehicle = new Vehicle();
        vehicle.setLogisticsId(logistics.getId());
        vehicle.setPlateNo(plateNo);
        vehicle.setVehicleType(vehicleType);
        vehicle.setLicenseNo(dto.getLicenseNo().trim());
        vehicle.setLicenseExpireDate(dto.getLicenseExpireDate());
        vehicle.setVehicleStatus(VehicleStatusEnum.NORMAL.getCode());
        vehicle.setAttachmentPaths(joinAttachmentPaths(dto.getAttachmentPaths()));
        vehicle.setCreateTime(clockService.now());
        vehicleMapper.insert(vehicle);

        VehicleCreateVO vo = new VehicleCreateVO();
        vo.setVehicleId(vehicle.getId());
        vo.setPlateNo(vehicle.getPlateNo());
        vo.setVehicleStatus(vehicle.getVehicleStatus());
        return vo;
    }

    /**
     * 维护本公司车辆状态，有运输中任务时禁止改为维修或停用。
     *
     * @param vehicleId 车辆 ID
     * @param dto 状态参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Long vehicleId, VehicleStatusDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        Vehicle vehicle = vehicleMapper.selectById(vehicleId);
        if (vehicle == null) {
            throw new BusinessException("车辆不存在");
        }
        if (!logistics.getId().equals(vehicle.getLogisticsId())) {
            throw new ForbiddenException("无权操作该车辆");
        }
        VehicleStatusEnum targetStatus = VehicleStatusEnum.of(dto.getStatus().trim());
        if (targetStatus == null) {
            throw new BusinessException("车辆状态不正确");
        }
        if (targetStatus != VehicleStatusEnum.NORMAL) {
            long running = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                    .eq(Task::getVehicleId, vehicle.getId())
                    .eq(Task::getTaskStatus, TaskStatusEnum.IN_TRANSPORT.getCode()));
            if (running > 0) {
                throw new BusinessException("车辆有进行中任务，不能停用或维修");
            }
        }
        vehicleMapper.update(null, new LambdaUpdateWrapper<Vehicle>()
                .set(Vehicle::getVehicleStatus, targetStatus.getCode())
                .eq(Vehicle::getId, vehicle.getId()));
    }

    /**
     * 查询当前司机绑定车辆，未绑定时返回空数据。
     *
     * @return 绑定车辆详情或 null
     */
    @Override
    public DriverVehicleVO getCurrentDriverVehicle() {
        Driver driver = currentAccountService.getDriver();
        if (driver.getVehicleId() == null) {
            return null;
        }
        Vehicle vehicle = vehicleMapper.selectById(driver.getVehicleId());
        if (vehicle == null) {
            return null;
        }
        DriverVehicleVO vo = new DriverVehicleVO();
        vo.setVehicleId(vehicle.getId());
        vo.setPlateNo(vehicle.getPlateNo());
        vo.setVehicleType(vehicle.getVehicleType());
        vo.setLicenseNo(vehicle.getLicenseNo());
        vo.setLicenseExpireDate(vehicle.getLicenseExpireDate());
        vo.setVehicleStatus(vehicle.getVehicleStatus());
        vo.setAttachmentPaths(vehicle.getAttachmentPaths());
        return vo;
    }

    /**
     * 获取当前物流公司资料。
     *
     * @param account 物流公司账号
     * @return 物流公司资料
     */
    private Logistics requireLogistics(Account account) {
        Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, account.getId()));
        if (logistics == null) {
            throw new BusinessException("物流公司资料不存在");
        }
        return logistics;
    }

    /**
     * 批量加载车辆绑定司机姓名。
     *
     * @param logistics 物流公司资料
     * @param vehicles 车辆列表
     * @return 车辆 ID 到司机姓名映射
     */
    private Map<Long, String> loadDriverNames(Logistics logistics, List<Vehicle> vehicles) {
        Set<Long> vehicleIds = vehicles.stream().map(Vehicle::getId).collect(Collectors.toSet());
        Map<Long, String> result = new HashMap<>();
        for (Driver driver : driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getLogisticsId, logistics.getId())
                .in(Driver::getVehicleId, vehicleIds))) {
            result.put(driver.getVehicleId(), driver.getName());
        }
        return result;
    }

    /**
     * 将附件路径列表拼接为英文逗号分隔字符串。
     *
     * @param attachmentPaths 附件路径列表
     * @return 拼接结果或 null
     */
    private String joinAttachmentPaths(List<String> attachmentPaths) {
        if (attachmentPaths == null) {
            return null;
        }
        String joined = attachmentPaths.stream().filter(path -> path != null && !path.isBlank())
                .map(String::trim).collect(Collectors.joining(","));
        return joined.isEmpty() ? null : joined;
    }
}
