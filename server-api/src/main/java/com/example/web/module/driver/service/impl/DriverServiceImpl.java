package com.example.web.module.driver.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.MaskUtil;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.driver.dto.DriverBindingDTO;
import com.example.web.module.driver.dto.DriverCreateDTO;
import com.example.web.module.driver.dto.DriverQueryDTO;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.enums.EmploymentStatusEnum;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.driver.service.DriverService;
import com.example.web.module.driver.vo.DriverCreateVO;
import com.example.web.module.driver.vo.DriverListVO;
import com.example.web.module.governance.enums.OperationActionEnum;
import com.example.web.module.governance.service.OperationLogService;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.realname.adapter.RealnameVerificationAdapter;
import com.example.web.module.realname.adapter.RealnameVerifyResult;
import com.example.web.module.realname.entity.Realname;
import com.example.web.module.realname.enums.RealnameTypeEnum;
import com.example.web.module.realname.enums.VerifyStatusEnum;
import com.example.web.module.realname.mapper.RealnameMapper;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.entity.Task;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.entity.VehicleBindingLog;
import com.example.web.module.vehicle.mapper.VehicleBindingLogMapper;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.security.CurrentAccountService;
import com.example.web.util.PasswordUtil;
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
 * 物流公司司机管理业务实现，创建司机时在同一事务内完成账号、实名与资料落库。
 */
@Service
public class DriverServiceImpl implements DriverService {

    private final CurrentAccountService currentAccountService;
    private final AccountMapper accountMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final VehicleBindingLogMapper vehicleBindingLogMapper;
    private final LogisticsMapper logisticsMapper;
    private final RealnameMapper realnameMapper;
    private final RealnameVerificationAdapter verificationAdapter;
    private final OperationLogService operationLogService;
    private final TaskMapper taskMapper;
    private final ClockService clockService;

    public DriverServiceImpl(CurrentAccountService currentAccountService, AccountMapper accountMapper,
                             DriverMapper driverMapper, VehicleMapper vehicleMapper,
                             VehicleBindingLogMapper vehicleBindingLogMapper, LogisticsMapper logisticsMapper,
                             RealnameMapper realnameMapper, RealnameVerificationAdapter verificationAdapter,
                             OperationLogService operationLogService, TaskMapper taskMapper,
                             ClockService clockService) {
        this.currentAccountService = currentAccountService;
        this.accountMapper = accountMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.vehicleBindingLogMapper = vehicleBindingLogMapper;
        this.logisticsMapper = logisticsMapper;
        this.realnameMapper = realnameMapper;
        this.verificationAdapter = verificationAdapter;
        this.operationLogService = operationLogService;
        this.taskMapper = taskMapper;
        this.clockService = clockService;
    }

    /**
     * 创建司机账号、实名校验、司机资料与操作留痕，任一步失败整体回滚。
     *
     * @param dto 司机创建参数
     * @return 创建结果
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public DriverCreateVO createDriver(DriverCreateDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        String phone = dto.getPhone().trim();
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, phone)) > 0) {
            throw new BusinessException("手机号已注册");
        }
        Vehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null || !logistics.getId().equals(vehicle.getLogisticsId())) {
            throw new BusinessException("车辆不属于本公司");
        }
        if (driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getVehicleId, vehicle.getId())) > 0) {
            throw new BusinessException("车辆已被其他司机绑定");
        }

        LocalDateTime now = clockService.now();
        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(AccountRoleEnum.DRIVER);
        account.setUsername(phone);
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(dto.getPassword(), salt));
        account.setStatus(EnableStatusEnum.ENABLED);
        account.setMustChangePassword(1);
        accountMapper.insert(account);

        String idNo = dto.getIdNo().trim();
        Realname existingIdentity = realnameMapper.selectOne(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                .eq(Realname::getIdType, dto.getIdType().getCode())
                .eq(Realname::getIdNo, idNo));
        if (existingIdentity != null) {
            throw new BusinessException("该实名身份已绑定其他账号");
        }
        RealnameVerifyResult verifyResult = verificationAdapter.verifyPersonal(dto.getName().trim(),
                dto.getIdType(), idNo);
        if (!verifyResult.passed()) {
            throw new BusinessException("司机实名校验未通过：" + verifyResult.failReason());
        }
        Realname realname = new Realname();
        realname.setAccountId(account.getId());
        realname.setRole(AccountRoleEnum.DRIVER.getCode());
        realname.setRealnameType(RealnameTypeEnum.PERSONAL.getCode());
        realname.setRealName(dto.getName().trim());
        realname.setIdType(dto.getIdType().getCode());
        realname.setIdNo(idNo);
        realname.setPhone(phone);
        realname.setVerifyStatus(VerifyStatusEnum.PASSED.getCode());
        realname.setVerifyTime(now);
        realname.setCreateTime(now);
        realnameMapper.insert(realname);

        Driver driver = new Driver();
        driver.setAccountId(account.getId());
        driver.setLogisticsId(logistics.getId());
        driver.setRealnameId(realname.getId());
        driver.setVehicleId(vehicle.getId());
        driver.setName(dto.getName().trim());
        driver.setPhone(phone);
        driver.setEmail(dto.getEmail());
        driver.setLicenseNo(dto.getLicenseNo().trim());
        driver.setLicenseExpireDate(dto.getLicenseExpireDate());
        driver.setEmploymentStatus(EmploymentStatusEnum.ACTIVE.getCode());
        driver.setCreateTime(now);
        driverMapper.insert(driver);

        operationLogService.record(operator.getId(), AccountRoleEnum.LOGISTICS, OperationActionEnum.DRIVER_CREATE,
                "DRIVER", driver.getId(), "创建司机" + driver.getName() + "，绑定车辆" + vehicle.getPlateNo());

        DriverCreateVO vo = new DriverCreateVO();
        vo.setDriverId(driver.getId());
        vo.setAccountId(account.getId());
        vo.setName(driver.getName());
        vo.setPhone(driver.getPhone());
        vo.setEmploymentStatus(driver.getEmploymentStatus());
        return vo;
    }

    /**
     * 分页查询本公司司机并补齐绑定车辆与脱敏证件号。
     *
     * @param dto 查询参数
     * @return 司机分页数据
     */
    @Override
    public PageResult<DriverListVO> pageDrivers(DriverQueryDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        LambdaQueryWrapper<Driver> wrapper = new LambdaQueryWrapper<Driver>()
                .eq(Driver::getLogisticsId, logistics.getId());
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            String keyword = dto.getKeyword().trim();
            wrapper.and(w -> w.like(Driver::getName, keyword).or().like(Driver::getPhone, keyword));
        }
        wrapper.orderByDesc(Driver::getId);
        Page<Driver> page = driverMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
        }

        Map<Long, Vehicle> vehicleMap = loadVehicles(page.getRecords());
        Map<Long, Realname> realnameMap = loadRealnames(page.getRecords());
        List<DriverListVO> records = new ArrayList<>();
        for (Driver driver : page.getRecords()) {
            DriverListVO vo = new DriverListVO();
            vo.setDriverId(driver.getId());
            vo.setAccountId(driver.getAccountId());
            vo.setName(driver.getName());
            vo.setPhone(driver.getPhone());
            Realname realname = realnameMap.get(driver.getRealnameId());
            if (realname != null) {
                vo.setIdNo(MaskUtil.maskIdNo(realname.getIdNo()));
            }
            vo.setLicenseNo(driver.getLicenseNo());
            vo.setLicenseExpireDate(driver.getLicenseExpireDate());
            vo.setVehicleId(driver.getVehicleId());
            Vehicle vehicle = vehicleMap.get(driver.getVehicleId());
            if (vehicle != null) {
                vo.setVehiclePlateNo(vehicle.getPlateNo());
            }
            vo.setEmploymentStatus(driver.getEmploymentStatus());
            vo.setCreateTime(driver.getCreateTime());
            records.add(vo);
        }
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 调整本公司司机绑定车辆，校验车辆归属、占用与司机进行中任务。
     *
     * @param driverId 司机资料 ID
     * @param dto 绑定参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void adjustBinding(Long driverId, DriverBindingDTO dto) {
        Account operator = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = requireLogistics(operator);
        Driver driver = driverMapper.selectById(driverId);
        if (driver == null || !logistics.getId().equals(driver.getLogisticsId())) {
            throw new ForbiddenException("无权操作该司机");
        }
        Vehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null || !logistics.getId().equals(vehicle.getLogisticsId())) {
            throw new BusinessException("车辆不属于本公司");
        }
        long occupied = driverMapper.selectCount(new LambdaQueryWrapper<Driver>()
                .eq(Driver::getVehicleId, vehicle.getId())
                .ne(Driver::getId, driver.getId()));
        if (occupied > 0) {
            throw new BusinessException("车辆已被其他司机绑定");
        }
        long runningTasks = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getDriverAccountId, driver.getAccountId())
                .in(Task::getTaskStatus, TaskStatusEnum.WAIT_EXECUTE.getCode(),
                        TaskStatusEnum.IN_TRANSPORT.getCode()));
        if (runningTasks > 0) {
            throw new BusinessException("司机有进行中任务，不能调整绑定");
        }

        Long oldVehicleId = driver.getVehicleId();
        LocalDateTime now = clockService.now();
        driverMapper.update(null, new LambdaUpdateWrapper<Driver>()
                .set(Driver::getVehicleId, vehicle.getId())
                .eq(Driver::getId, driver.getId()));
        VehicleBindingLog bindingLog = new VehicleBindingLog();
        bindingLog.setDriverId(driver.getId());
        bindingLog.setOldVehicleId(oldVehicleId);
        bindingLog.setNewVehicleId(vehicle.getId());
        bindingLog.setOperatorAccountId(operator.getId());
        bindingLog.setCreateTime(now);
        vehicleBindingLogMapper.insert(bindingLog);

        operationLogService.record(operator.getId(), AccountRoleEnum.LOGISTICS,
                OperationActionEnum.DRIVER_BINDING_ADJUST, "DRIVER", driver.getId(),
                "司机" + driver.getName() + "车辆绑定调整为" + vehicle.getPlateNo());
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
     * 批量加载司机绑定车辆。
     *
     * @param drivers 司机列表
     * @return 车辆 ID 到车辆映射
     */
    private Map<Long, Vehicle> loadVehicles(List<Driver> drivers) {
        Set<Long> vehicleIds = drivers.stream().map(Driver::getVehicleId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Vehicle> result = new HashMap<>();
        for (Vehicle vehicle : vehicleMapper.selectBatchIds(vehicleIds)) {
            result.put(vehicle.getId(), vehicle);
        }
        return result;
    }

    /**
     * 批量加载司机实名记录。
     *
     * @param drivers 司机列表
     * @return 实名 ID 到实名记录映射
     */
    private Map<Long, Realname> loadRealnames(List<Driver> drivers) {
        Set<Long> realnameIds = drivers.stream().map(Driver::getRealnameId)
                .filter(id -> id != null).collect(Collectors.toSet());
        if (realnameIds.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Realname> result = new HashMap<>();
        for (Realname realname : realnameMapper.selectBatchIds(realnameIds)) {
            result.put(realname.getId(), realname);
        }
        return result;
    }
}
