package com.example.web.module.governance.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import com.example.web.module.checkin.vo.TaskCheckinSummaryVO;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.governance.dto.AdminCheckinQueryDTO;
import com.example.web.module.governance.dto.AdminOrderQueryDTO;
import com.example.web.module.governance.dto.AdminTaskQueryDTO;
import com.example.web.module.governance.dto.DictItemSaveDTO;
import com.example.web.module.governance.dto.OperationLogQueryDTO;
import com.example.web.module.governance.dto.PlatformParamSaveDTO;
import com.example.web.module.governance.entity.DictItem;
import com.example.web.module.governance.entity.OperationLog;
import com.example.web.module.governance.entity.PlatformParam;
import com.example.web.module.governance.enums.DictTypeEnum;
import com.example.web.module.governance.enums.OperationActionEnum;
import com.example.web.module.governance.mapper.DictItemMapper;
import com.example.web.module.governance.mapper.OperationLogMapper;
import com.example.web.module.governance.mapper.PlatformParamMapper;
import com.example.web.module.governance.service.AdminGovernanceService;
import com.example.web.module.governance.service.OperationLogService;
import com.example.web.module.governance.service.PlatformParamService;
import com.example.web.module.governance.vo.AdminOrderDetailVO;
import com.example.web.module.governance.vo.AdminOrderListVO;
import com.example.web.module.governance.vo.AdminOrderTaskVO;
import com.example.web.module.governance.vo.AdminTaskListVO;
import com.example.web.module.governance.vo.DictItemVO;
import com.example.web.module.governance.vo.OperationLogVO;
import com.example.web.module.governance.vo.PlatformParamVO;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.entity.OrderAddress;
import com.example.web.module.order.entity.OrderGoods;
import com.example.web.module.order.enums.AddressTypeEnum;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderAddressMapper;
import com.example.web.module.order.mapper.OrderGoodsMapper;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderQueryService;
import com.example.web.module.order.vo.OrderAddressVO;
import com.example.web.module.order.vo.OrderGoodsVO;
import com.example.web.module.realname.entity.Realname;
import com.example.web.module.realname.enums.RealnameTypeEnum;
import com.example.web.module.realname.mapper.RealnameMapper;
import com.example.web.module.reimbursement.service.ReimbursementService;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskQueryService;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员治理业务实现，所有业务数据只读，仅基础资料、平台参数与操作留痕允许写入。
 */
@Service
public class AdminGovernanceServiceImpl implements AdminGovernanceService {

    /**
     * 时间格式。
     */
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final CurrentAccountService currentAccountService;
    private final OrderQueryService orderQueryService;
    private final TaskQueryService taskQueryService;
    private final ReimbursementService reimbursementService;
    private final OperationLogService operationLogService;
    private final PlatformParamService platformParamService;
    private final ClockService clockService;
    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final TaskMapper taskMapper;
    private final DriverMapper driverMapper;
    private final LogisticsMapper logisticsMapper;
    private final ShipperMapper shipperMapper;
    private final VehicleMapper vehicleMapper;
    private final RealnameMapper realnameMapper;
    private final OperationLogMapper operationLogMapper;
    private final DictItemMapper dictItemMapper;
    private final PlatformParamMapper platformParamMapper;

    public AdminGovernanceServiceImpl(CurrentAccountService currentAccountService,
                                      OrderQueryService orderQueryService,
                                      TaskQueryService taskQueryService,
                                      ReimbursementService reimbursementService,
                                      OperationLogService operationLogService,
                                      PlatformParamService platformParamService, ClockService clockService,
                                      OrderMapper orderMapper, OrderGoodsMapper orderGoodsMapper,
                                      OrderAddressMapper orderAddressMapper, TaskMapper taskMapper,
                                      DriverMapper driverMapper, LogisticsMapper logisticsMapper,
                                      ShipperMapper shipperMapper, VehicleMapper vehicleMapper,
                                      RealnameMapper realnameMapper, OperationLogMapper operationLogMapper,
                                      DictItemMapper dictItemMapper, PlatformParamMapper platformParamMapper) {
        this.currentAccountService = currentAccountService;
        this.orderQueryService = orderQueryService;
        this.taskQueryService = taskQueryService;
        this.reimbursementService = reimbursementService;
        this.operationLogService = operationLogService;
        this.platformParamService = platformParamService;
        this.clockService = clockService;
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.taskMapper = taskMapper;
        this.driverMapper = driverMapper;
        this.logisticsMapper = logisticsMapper;
        this.shipperMapper = shipperMapper;
        this.vehicleMapper = vehicleMapper;
        this.realnameMapper = realnameMapper;
        this.operationLogMapper = operationLogMapper;
        this.dictItemMapper = dictItemMapper;
        this.platformParamMapper = platformParamMapper;
    }

    /**
     * 分页查询全平台订单。
     *
     * @param dto 查询参数
     * @return 订单分页数据
     */
    @Override
    public PageResult<AdminOrderListVO> pageOrders(AdminOrderQueryDTO dto) {
        requireAdmin();
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<>();
        if (dto.getShipperAccountId() != null) {
            wrapper.eq(Order::getShipperAccountId, dto.getShipperAccountId());
        }
        if (dto.getLogisticsAccountId() != null) {
            wrapper.eq(Order::getLogisticsAccountId, dto.getLogisticsAccountId());
        }
        if (dto.getOrderStatus() != null && !dto.getOrderStatus().isBlank()) {
            OrderStatusEnum status = OrderStatusEnum.of(dto.getOrderStatus().trim());
            if (status == null) {
                throw new BusinessException("订单状态不正确");
            }
            wrapper.eq(Order::getOrderStatus, status.getCode());
        }
        LocalDateTime startTime = parseDateTime(dto.getStartTime(), false);
        LocalDateTime endTime = parseDateTime(dto.getEndTime(), true);
        if (startTime != null) {
            wrapper.ge(Order::getCreateTime, startTime);
        }
        if (endTime != null) {
            wrapper.le(Order::getCreateTime, endTime);
        }
        wrapper.orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
        }
        List<Order> records = page.getRecords();
        Map<Long, List<OrderGoods>> goodsMap = loadGoodsMap(records);
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(records);
        Map<Long, Long> taskCountMap = loadTaskCounts(records);
        Map<Long, String> shipperNames = loadShipperNames(records);
        Map<Long, String> logisticsNames = loadLogisticsNames(records);
        List<AdminOrderListVO> result = new ArrayList<>();
        for (Order order : records) {
            AdminOrderListVO vo = new AdminOrderListVO();
            vo.setId(order.getId());
            vo.setOrderNo(order.getOrderNo());
            vo.setWaybillNo(order.getWaybillNo());
            vo.setOrderStatus(order.getOrderStatus());
            vo.setShipperAccountId(order.getShipperAccountId());
            vo.setShipperName(shipperNames.get(order.getShipperAccountId()));
            vo.setLogisticsAccountId(order.getLogisticsAccountId());
            vo.setLogisticsName(logisticsNames.get(order.getLogisticsAccountId()));
            vo.setFreightAmount(order.getFreightAmount());
            fillRegion(vo, addressMap.get(order.getId()));
            vo.setGoodsCount(goodsMap.getOrDefault(order.getId(), Collections.emptyList()).size());
            vo.setTaskCount(taskCountMap.getOrDefault(order.getId(), 0L).intValue());
            vo.setPublishTime(order.getPublishTime());
            vo.setExpireTime(order.getExpireTime());
            vo.setPickupTime(order.getPickupTime());
            vo.setDeliveryTime(order.getDeliveryTime());
            vo.setAcceptTime(order.getAcceptTime());
            vo.setCreateTime(order.getCreateTime());
            result.add(vo);
        }
        return new PageResult<>(result, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询订单全链路详情。
     *
     * @param id 订单 ID
     * @return 全链路详情
     */
    @Override
    public AdminOrderDetailVO getOrderDetail(Long id) {
        requireAdmin();
        Order order = orderQueryService.getOrder(id);
        List<OrderGoods> goods = orderQueryService.listGoods(id);
        List<OrderAddress> addresses = orderQueryService.listAddresses(id);
        List<Task> tasks = orderQueryService.listOrderTasks(id);
        Map<Long, TaskCheckinSummaryVO> summaries = taskQueryService.getCheckinSummaries(
                tasks.stream().map(Task::getId).toList());

        AdminOrderDetailVO vo = new AdminOrderDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setShipperAccountId(order.getShipperAccountId());
        Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                .eq(Shipper::getAccountId, order.getShipperAccountId()));
        if (shipper != null) {
            vo.setShipperName(shipper.getName());
            vo.setShipperPhone(shipper.getPhone());
        }
        vo.setShipperIdNo(findPersonalIdNo(order.getShipperAccountId()));
        vo.setLogisticsAccountId(order.getLogisticsAccountId());
        Logistics logistics = order.getLogisticsAccountId() == null ? null
                : logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, order.getLogisticsAccountId()));
        if (logistics != null) {
            vo.setLogisticsName(logistics.getName());
            vo.setLogisticsContactPhone(logistics.getPhone());
        }
        if (order.getLogisticsAccountId() != null) {
            Onboarding onboarding = orderQueryService.getApprovedOnboarding(order.getLogisticsAccountId());
            if (onboarding != null) {
                vo.setLogisticsContactName(onboarding.getContactName());
                vo.setLogisticsContactPhone(onboarding.getContactPhone());
            }
            vo.setLogisticsIdNo(findPersonalIdNo(order.getLogisticsAccountId()));
        }
        vo.setFreightAmount(order.getFreightAmount());
        vo.setValidHours(order.getValidHours());
        vo.setRemark(order.getRemark());
        vo.setPublishTime(order.getPublishTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setAcceptTime(order.getAcceptTime());
        vo.setSourceOrderId(order.getSourceOrderId());
        vo.setRepublishCount(order.getRepublishCount());
        vo.setWithdrawTime(order.getWithdrawTime());
        vo.setCancelReason(order.getCancelReason());
        vo.setCancelTime(order.getCancelTime());
        vo.setOffshelfTime(order.getOffshelfTime());
        vo.setCreateTime(order.getCreateTime());
        vo.setGoods(toGoodsVOs(goods));
        fillAddresses(vo, addresses);
        vo.setTasks(toAdminOrderTasks(tasks, summaries));
        vo.setReimbursements(reimbursementService.listReimbursementsByOrder(id));
        return vo;
    }

    /**
     * 分页查询全平台任务。
     *
     * @param dto 查询参数
     * @return 任务分页数据
     */
    @Override
    public PageResult<AdminTaskListVO> pageTasks(AdminTaskQueryDTO dto) {
        requireAdmin();
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<>();
        if (dto.getOrderId() != null) {
            wrapper.eq(Task::getOrderId, dto.getOrderId());
        }
        if (dto.getLogisticsAccountId() != null) {
            wrapper.eq(Task::getLogisticsAccountId, dto.getLogisticsAccountId());
        }
        if (dto.getDriverAccountId() != null) {
            wrapper.eq(Task::getDriverAccountId, dto.getDriverAccountId());
        }
        if (dto.getTaskStatus() != null && !dto.getTaskStatus().isBlank()) {
            TaskStatusEnum status = TaskStatusEnum.of(dto.getTaskStatus().trim());
            if (status == null) {
                throw new BusinessException("任务状态不正确");
            }
            wrapper.eq(Task::getTaskStatus, status.getCode());
        }
        wrapper.orderByDesc(Task::getId);
        Page<Task> page = taskMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
        }
        List<Task> records = page.getRecords();
        Map<Long, TaskCheckinSummaryVO> summaries = taskQueryService.getCheckinSummaries(
                records.stream().map(Task::getId).toList());
        Map<Long, Driver> drivers = loadDrivers(records);
        Map<Long, String> logisticsNames = loadLogisticsNamesByTask(records);
        Map<Long, Order> orders = loadOrdersByTask(records);
        Map<Long, String> plateNos = loadPlateNos(records);
        List<AdminTaskListVO> result = new ArrayList<>();
        for (Task task : records) {
            AdminTaskListVO vo = new AdminTaskListVO();
            vo.setTaskId(task.getId());
            vo.setTaskNo(task.getTaskNo());
            vo.setOrderId(task.getOrderId());
            Order order = orders.get(task.getOrderId());
            if (order != null) {
                vo.setOrderNo(order.getOrderNo());
                vo.setWaybillNo(order.getWaybillNo());
            }
            vo.setLogisticsAccountId(task.getLogisticsAccountId());
            vo.setLogisticsName(logisticsNames.get(task.getLogisticsAccountId()));
            vo.setDriverAccountId(task.getDriverAccountId());
            Driver driver = drivers.get(task.getDriverAccountId());
            if (driver != null) {
                vo.setDriverName(driver.getName());
                vo.setDriverPhone(driver.getPhone());
            }
            vo.setVehicleId(task.getVehicleId());
            vo.setVehiclePlateNo(plateNos.get(task.getVehicleId()));
            vo.setTaskStatus(task.getTaskStatus());
            vo.setAssignType(task.getAssignType());
            vo.setOpenClaim(task.getOpenClaim());
            vo.setDriverAck(task.getDriverAck());
            vo.setDriverAckTime(task.getDriverAckTime());
            TaskCheckinSummaryVO summary = summaries.get(task.getId());
            vo.setDepartChecked(summary != null && !summary.getDepartVersions().isEmpty());
            vo.setArriveChecked(summary != null && !summary.getArriveVersions().isEmpty());
            vo.setCreateTime(task.getCreateTime());
            result.add(vo);
        }
        return new PageResult<>(result, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 分页查询全平台打卡版本。
     *
     * @param dto 查询参数
     * @return 打卡分页数据
     */
    @Override
    public PageResult<CheckinVersionVO> pageCheckins(AdminCheckinQueryDTO dto) {
        requireAdmin();
        return taskQueryService.pageCheckinVersions(dto.getTaskId(), dto.getOrderId(), dto.getNode(),
                dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 分页查询操作留痕。
     *
     * @param dto 查询参数
     * @return 操作留痕分页数据
     */
    @Override
    public PageResult<OperationLogVO> pageOperationLogs(OperationLogQueryDTO dto) {
        requireAdmin();
        LambdaQueryWrapper<OperationLog> wrapper = new LambdaQueryWrapper<>();
        if (dto.getActionType() != null && !dto.getActionType().isBlank()) {
            OperationActionEnum action = OperationActionEnum.of(dto.getActionType().trim());
            if (action == null) {
                throw new BusinessException("操作类型不正确");
            }
            wrapper.eq(OperationLog::getActionType, action.getCode());
        }
        if (dto.getOperatorAccountId() != null) {
            wrapper.eq(OperationLog::getOperatorAccountId, dto.getOperatorAccountId());
        }
        wrapper.orderByDesc(OperationLog::getId);
        Page<OperationLog> page = operationLogMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);
        List<OperationLogVO> records = page.getRecords().stream().map(this::toOperationLogVO).toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询字典项。
     *
     * @param dictType 字典类型
     * @param status 状态
     * @return 字典项列表
     */
    @Override
    public List<DictItemVO> listDictItems(String dictType, String status) {
        requireAdmin();
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<>();
        if (dictType != null && !dictType.isBlank()) {
            wrapper.eq(DictItem::getDictType, requireDictType(dictType).getCode());
        }
        if (status != null && !status.isBlank()) {
            wrapper.eq(DictItem::getStatus, requireEnableStatus(status).getCode());
        }
        wrapper.orderByAsc(DictItem::getDictType).orderByAsc(DictItem::getSortNo).orderByAsc(DictItem::getId);
        return dictItemMapper.selectList(wrapper).stream().map(this::toDictItemVO).toList();
    }

    /**
     * 新增或修改字典项并记录操作留痕。
     *
     * @param dto 字典项参数
     * @return 字典项 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long saveDictItem(DictItemSaveDTO dto) {
        Long adminAccountId = requireAdmin();
        DictTypeEnum dictType = requireDictType(dto.getDictType());
        EnableStatusEnum status = requireEnableStatus(dto.getStatus());
        String itemCode = dto.getItemCode().trim();
        String itemName = dto.getItemName().trim();
        Integer sortNo = dto.getSortNo() == null ? 0 : dto.getSortNo();
        String parentCode = trimToNull(dto.getParentCode());
        if (dto.getId() == null) {
            ensureDictCodeUnique(dictType, itemCode, null);
            DictItem item = new DictItem();
            item.setDictType(dictType.getCode());
            item.setItemCode(itemCode);
            item.setItemName(itemName);
            item.setParentCode(parentCode);
            item.setSortNo(sortNo);
            item.setStatus(status.getCode());
            item.setCreateTime(clockService.now());
            dictItemMapper.insert(item);
            operationLogService.record(adminAccountId, AccountRoleEnum.ADMIN,
                    OperationActionEnum.DICT_CREATE, "DICT", item.getId(),
                    "新增字典项：" + dictType.getCode() + "/" + itemCode);
            return item.getId();
        }
        DictItem existing = dictItemMapper.selectById(dto.getId());
        if (existing == null) {
            throw new BusinessException("字典项不存在");
        }
        ensureDictCodeUnique(dictType, itemCode, existing.getId());
        dictItemMapper.update(null, new LambdaUpdateWrapper<DictItem>()
                .set(DictItem::getDictType, dictType.getCode())
                .set(DictItem::getItemCode, itemCode)
                .set(DictItem::getItemName, itemName)
                .set(DictItem::getParentCode, parentCode)
                .set(DictItem::getSortNo, sortNo)
                .set(DictItem::getStatus, status.getCode())
                .eq(DictItem::getId, existing.getId()));
        operationLogService.record(adminAccountId, AccountRoleEnum.ADMIN,
                OperationActionEnum.DICT_UPDATE, "DICT", existing.getId(),
                "修改字典项：" + dictType.getCode() + "/" + itemCode);
        return existing.getId();
    }

    /**
     * 查询全部平台参数。
     *
     * @return 平台参数列表
     */
    @Override
    public List<PlatformParamVO> listPlatformParams() {
        requireAdmin();
        return platformParamService.listAll().stream().map(this::toPlatformParamVO).toList();
    }

    /**
     * 新增或修改平台参数并记录操作留痕。
     *
     * @param dto 平台参数
     * @return 参数 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long savePlatformParam(PlatformParamSaveDTO dto) {
        Long adminAccountId = requireAdmin();
        String paramKey = dto.getParamKey().trim();
        String paramValue = dto.getParamValue().trim();
        String description = trimToNull(dto.getDescription());
        PlatformParam existing = platformParamMapper.selectOne(new LambdaQueryWrapper<PlatformParam>()
                .eq(PlatformParam::getParamKey, paramKey));
        if (existing == null) {
            PlatformParam param = new PlatformParam();
            param.setParamKey(paramKey);
            param.setParamValue(paramValue);
            param.setDescription(description);
            param.setCreateTime(clockService.now());
            platformParamMapper.insert(param);
            recordParamUpdate(adminAccountId, param.getId(), paramKey, paramValue);
            return param.getId();
        }
        platformParamMapper.update(null, new LambdaUpdateWrapper<PlatformParam>()
                .set(PlatformParam::getParamValue, paramValue)
                .set(PlatformParam::getDescription, description)
                .eq(PlatformParam::getId, existing.getId()));
        recordParamUpdate(adminAccountId, existing.getId(), paramKey, paramValue);
        return existing.getId();
    }

    /**
     * 校验当前登录账号为管理员。
     *
     * @return 管理员账号 ID
     */
    private Long requireAdmin() {
        return currentAccountService.requireRole(AccountRoleEnum.ADMIN).getId();
    }

    /**
     * 校验字典类型。
     *
     * @param dictType 字典类型编码
     * @return 字典类型枚举
     */
    private DictTypeEnum requireDictType(String dictType) {
        DictTypeEnum type = DictTypeEnum.of(dictType == null ? null : dictType.trim());
        if (type == null) {
            throw new BusinessException("字典类型不正确");
        }
        return type;
    }

    /**
     * 校验启用状态。
     *
     * @param status 状态编码
     * @return 状态枚举
     */
    private EnableStatusEnum requireEnableStatus(String status) {
        if (status == null || status.isBlank()) {
            throw new BusinessException("状态不正确");
        }
        String code = status.trim();
        for (EnableStatusEnum item : EnableStatusEnum.values()) {
            if (item.getCode().equals(code)) {
                return item;
            }
        }
        throw new BusinessException("状态不正确");
    }

    /**
     * 校验同一字典类型下编码唯一。
     *
     * @param dictType 字典类型
     * @param itemCode 字典项编码
     * @param excludeId 排除的字典项 ID
     */
    private void ensureDictCodeUnique(DictTypeEnum dictType, String itemCode, Long excludeId) {
        LambdaQueryWrapper<DictItem> wrapper = new LambdaQueryWrapper<DictItem>()
                .eq(DictItem::getDictType, dictType.getCode())
                .eq(DictItem::getItemCode, itemCode);
        if (excludeId != null) {
            wrapper.ne(DictItem::getId, excludeId);
        }
        if (dictItemMapper.selectCount(wrapper) > 0) {
            throw new BusinessException("同一字典类型下字典编码已存在");
        }
    }

    /**
     * 记录平台参数修改留痕。
     *
     * @param adminAccountId 管理员账号 ID
     * @param paramId 参数 ID
     * @param paramKey 参数键
     * @param paramValue 参数值
     */
    private void recordParamUpdate(Long adminAccountId, Long paramId, String paramKey, String paramValue) {
        operationLogService.record(adminAccountId, AccountRoleEnum.ADMIN,
                OperationActionEnum.PARAM_UPDATE, "PLATFORM_PARAM", paramId,
                "更新参数：" + paramKey + "=" + paramValue);
    }

    /**
     * 解析时间参数，支持日期与完整时间，终止时间按当日结束。
     *
     * @param value 时间文本
     * @param endOfDay 是否补足到当日结束
     * @return 时间，未传时返回 null
     */
    private LocalDateTime parseDateTime(String value, boolean endOfDay) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String trimmed = value.trim();
        try {
            return LocalDateTime.parse(trimmed, DATE_TIME_FORMATTER);
        } catch (DateTimeParseException exception) {
            try {
                return LocalDateTime.parse(trimmed + (endOfDay ? " 23:59:59" : " 00:00:00"),
                        DATE_TIME_FORMATTER);
            } catch (DateTimeParseException ignored) {
                throw new BusinessException("时间格式不正确，应为 yyyy-MM-dd HH:mm:ss");
            }
        }
    }

    /**
     * 加载订单货物。
     *
     * @param orders 订单列表
     * @return 订单 ID 到货物列表映射
     */
    private Map<Long, List<OrderGoods>> loadGoodsMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .in(OrderGoods::getOrderId, orderIds)
                        .orderByAsc(OrderGoods::getId))
                .stream().collect(Collectors.groupingBy(OrderGoods::getOrderId));
    }

    /**
     * 加载订单地址。
     *
     * @param orders 订单列表
     * @return 订单 ID 到地址列表映射
     */
    private Map<Long, List<OrderAddress>> loadAddressMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                        .in(OrderAddress::getOrderId, orderIds)
                        .orderByAsc(OrderAddress::getId))
                .stream().collect(Collectors.groupingBy(OrderAddress::getOrderId));
    }

    /**
     * 统计订单任务数。
     *
     * @param orders 订单列表
     * @return 订单 ID 到任务数映射
     */
    private Map<Long, Long> loadTaskCounts(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        Map<Long, Long> counts = new HashMap<>();
        for (Task task : taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .in(Task::getOrderId, orderIds))) {
            counts.merge(task.getOrderId(), 1L, Long::sum);
        }
        return counts;
    }

    /**
     * 加载货主名称。
     *
     * @param orders 订单列表
     * @return 货主账号 ID 到名称映射
     */
    private Map<Long, String> loadShipperNames(List<Order> orders) {
        List<Long> accountIds = orders.stream().map(Order::getShipperAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return shipperMapper.selectList(new LambdaQueryWrapper<Shipper>()
                        .in(Shipper::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Shipper::getAccountId, Shipper::getName,
                        (first, second) -> first));
    }

    /**
     * 加载物流公司名称。
     *
     * @param orders 订单列表
     * @return 物流公司账号 ID 到名称映射
     */
    private Map<Long, String> loadLogisticsNames(List<Order> orders) {
        List<Long> accountIds = orders.stream().map(Order::getLogisticsAccountId)
                .filter(id -> id != null).distinct().toList();
        return loadLogisticsNamesByIds(accountIds);
    }

    /**
     * 加载任务所属物流公司名称。
     *
     * @param tasks 任务列表
     * @return 物流公司账号 ID 到名称映射
     */
    private Map<Long, String> loadLogisticsNamesByTask(List<Task> tasks) {
        List<Long> accountIds = tasks.stream().map(Task::getLogisticsAccountId)
                .filter(id -> id != null).distinct().toList();
        return loadLogisticsNamesByIds(accountIds);
    }

    /**
     * 按账号 ID 批量加载物流公司名称。
     *
     * @param accountIds 账号 ID 列表
     * @return 物流公司账号 ID 到名称映射
     */
    private Map<Long, String> loadLogisticsNamesByIds(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>()
                        .in(Logistics::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Logistics::getAccountId, Logistics::getName,
                        (first, second) -> first));
    }

    /**
     * 加载任务司机资料。
     *
     * @param tasks 任务列表
     * @return 司机账号 ID 到司机资料映射
     */
    private Map<Long, Driver> loadDrivers(List<Task> tasks) {
        List<Long> accountIds = tasks.stream().map(Task::getDriverAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                        .in(Driver::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Driver::getAccountId, driver -> driver,
                        (first, second) -> first));
    }

    /**
     * 加载任务关联订单。
     *
     * @param tasks 任务列表
     * @return 订单 ID 到订单映射
     */
    private Map<Long, Order> loadOrdersByTask(List<Task> tasks) {
        List<Long> orderIds = tasks.stream().map(Task::getOrderId)
                .filter(id -> id != null).distinct().toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderMapper.selectBatchIds(orderIds).stream()
                .collect(Collectors.toMap(Order::getId, order -> order, (first, second) -> first));
    }

    /**
     * 加载任务车辆车牌号。
     *
     * @param tasks 任务列表
     * @return 车辆 ID 到车牌号映射
     */
    private Map<Long, String> loadPlateNos(List<Task> tasks) {
        List<Long> vehicleIds = tasks.stream().map(Task::getVehicleId)
                .filter(id -> id != null).distinct().toList();
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlateNo, (first, second) -> first));
    }

    /**
     * 查询账号个人实名完整证件号，仅管理员使用。
     *
     * @param accountId 账号 ID
     * @return 完整证件号，未实名时返回 null
     */
    private String findPersonalIdNo(Long accountId) {
        if (accountId == null) {
            return null;
        }
        List<Realname> realnames = realnameMapper.selectList(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getAccountId, accountId)
                .eq(Realname::getRealnameType, RealnameTypeEnum.PERSONAL.getCode())
                .orderByDesc(Realname::getId));
        return realnames.isEmpty() ? null : realnames.get(0).getIdNo();
    }

    /**
     * 填充订单列表省市概要。
     *
     * @param vo 订单列表项
     * @param addresses 订单地址
     */
    private void fillRegion(AdminOrderListVO vo, List<OrderAddress> addresses) {
        if (addresses == null) {
            return;
        }
        for (OrderAddress address : addresses) {
            if (AddressTypeEnum.PICKUP.getCode().equals(address.getAddressType())) {
                vo.setPickupProvince(address.getProvince());
                vo.setPickupCity(address.getCity());
            } else if (AddressTypeEnum.DELIVERY.getCode().equals(address.getAddressType())) {
                vo.setDeliveryProvince(address.getProvince());
                vo.setDeliveryCity(address.getCity());
            }
        }
    }

    /**
     * 填充装卸货完整地址。
     *
     * @param vo 订单详情
     * @param addresses 订单地址
     */
    private void fillAddresses(AdminOrderDetailVO vo, List<OrderAddress> addresses) {
        for (OrderAddress address : addresses) {
            OrderAddressVO addressVO = new OrderAddressVO();
            addressVO.setAddressType(address.getAddressType());
            addressVO.setProvince(address.getProvince());
            addressVO.setCity(address.getCity());
            addressVO.setDistrict(address.getDistrict());
            addressVO.setDetail(address.getDetail());
            addressVO.setContactName(address.getContactName());
            addressVO.setContactPhone(address.getContactPhone());
            if (AddressTypeEnum.PICKUP.getCode().equals(address.getAddressType())) {
                vo.setPickup(addressVO);
            } else if (AddressTypeEnum.DELIVERY.getCode().equals(address.getAddressType())) {
                vo.setDelivery(addressVO);
            }
        }
    }

    /**
     * 转换订单货物明细。
     *
     * @param goods 货物列表
     * @return 货物展示项
     */
    private List<OrderGoodsVO> toGoodsVOs(List<OrderGoods> goods) {
        List<OrderGoodsVO> result = new ArrayList<>();
        for (OrderGoods item : goods) {
            OrderGoodsVO vo = new OrderGoodsVO();
            vo.setId(item.getId());
            vo.setGoodsName(item.getGoodsName());
            vo.setGoodsType(item.getGoodsType());
            vo.setQuantity(item.getQuantity());
            vo.setWeight(item.getWeight());
            vo.setVolume(item.getVolume());
            vo.setUnit(item.getUnit());
            vo.setRemark(item.getRemark());
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换订单任务与打卡凭证。
     *
     * @param tasks 任务列表
     * @param summaries 打卡汇总
     * @return 管理员任务视图
     */
    private List<AdminOrderTaskVO> toAdminOrderTasks(List<Task> tasks,
                                                     Map<Long, TaskCheckinSummaryVO> summaries) {
        Map<Long, Driver> drivers = loadDrivers(tasks);
        Map<Long, String> plateNos = loadPlateNos(tasks);
        List<AdminOrderTaskVO> result = new ArrayList<>();
        for (Task task : tasks) {
            AdminOrderTaskVO vo = new AdminOrderTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskNo(task.getTaskNo());
            vo.setTaskStatus(task.getTaskStatus());
            vo.setLogisticsAccountId(task.getLogisticsAccountId());
            vo.setDriverAccountId(task.getDriverAccountId());
            Driver driver = drivers.get(task.getDriverAccountId());
            if (driver != null) {
                vo.setDriverName(driver.getName());
                vo.setDriverPhone(driver.getPhone());
                vo.setDriverLicenseNo(driver.getLicenseNo());
                Realname realname = driver.getRealnameId() == null ? null
                        : realnameMapper.selectById(driver.getRealnameId());
                vo.setDriverIdNo(realname == null ? null : realname.getIdNo());
            }
            vo.setVehicleId(task.getVehicleId());
            vo.setVehiclePlateNo(plateNos.get(task.getVehicleId()));
            vo.setAssignType(task.getAssignType());
            vo.setOpenClaim(task.getOpenClaim());
            vo.setDriverAck(task.getDriverAck());
            vo.setDriverAckTime(task.getDriverAckTime());
            vo.setCreateTime(task.getCreateTime());
            TaskCheckinSummaryVO summary = summaries.get(task.getId());
            vo.setDepartCheckins(summary == null ? Collections.emptyList() : summary.getDepartVersions());
            vo.setArriveCheckins(summary == null ? Collections.emptyList() : summary.getArriveVersions());
            vo.setRemarks(summary == null ? Collections.<CheckinRemarkVO>emptyList() : summary.getRemarks());
            result.add(vo);
        }
        return result;
    }

    /**
     * 转换操作留痕。
     *
     * @param log 留痕实体
     * @return 留痕展示项
     */
    private OperationLogVO toOperationLogVO(OperationLog log) {
        OperationLogVO vo = new OperationLogVO();
        vo.setId(log.getId());
        vo.setOperatorAccountId(log.getOperatorAccountId());
        vo.setOperatorRole(log.getOperatorRole());
        vo.setActionType(log.getActionType());
        OperationActionEnum action = OperationActionEnum.of(log.getActionType());
        vo.setActionTypeName(action == null ? null : action.getName());
        vo.setTargetType(log.getTargetType());
        vo.setTargetId(log.getTargetId());
        vo.setDetail(log.getDetail());
        vo.setCreateTime(log.getCreateTime());
        return vo;
    }

    /**
     * 转换字典项。
     *
     * @param item 字典项实体
     * @return 字典项展示信息
     */
    private DictItemVO toDictItemVO(DictItem item) {
        DictItemVO vo = new DictItemVO();
        vo.setId(item.getId());
        vo.setDictType(item.getDictType());
        vo.setItemCode(item.getItemCode());
        vo.setItemName(item.getItemName());
        vo.setParentCode(item.getParentCode());
        vo.setSortNo(item.getSortNo());
        vo.setStatus(item.getStatus());
        vo.setCreateTime(item.getCreateTime());
        return vo;
    }

    /**
     * 转换平台参数。
     *
     * @param param 参数实体
     * @return 参数展示信息
     */
    private PlatformParamVO toPlatformParamVO(PlatformParam param) {
        PlatformParamVO vo = new PlatformParamVO();
        vo.setId(param.getId());
        vo.setParamKey(param.getParamKey());
        vo.setParamValue(param.getParamValue());
        vo.setDescription(param.getDescription());
        vo.setCreateTime(param.getCreateTime());
        return vo;
    }

    /**
     * 去除首尾空白，空白字符串返回 null。
     *
     * @param value 原始字符串
     * @return 处理后字符串
     */
    private String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
