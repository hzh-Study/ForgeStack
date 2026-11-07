package com.example.web.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.enums.EmploymentStatusEnum;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.entity.OrderAddress;
import com.example.web.module.order.entity.OrderGoods;
import com.example.web.module.order.enums.AddressTypeEnum;
import com.example.web.module.order.mapper.OrderAddressMapper;
import com.example.web.module.order.mapper.OrderGoodsMapper;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderQueryService;
import com.example.web.module.order.vo.OrderAddressVO;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.dto.TaskDispatchDTO;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.entity.TaskEvent;
import com.example.web.module.task.entity.TaskGoods;
import com.example.web.module.task.enums.AssignTypeEnum;
import com.example.web.module.task.enums.TaskEventTypeEnum;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskEventMapper;
import com.example.web.module.task.mapper.TaskGoodsMapper;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskDispatchService;
import com.example.web.module.task.service.TaskQueryService;
import com.example.web.module.task.vo.DriverTaskDetailVO;
import com.example.web.module.task.vo.DriverTaskListVO;
import com.example.web.module.task.vo.DriverTaskPoolVO;
import com.example.web.module.task.vo.TaskBriefVO;
import com.example.web.module.task.vo.TaskGoodsItemVO;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.enums.VehicleStatusEnum;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 任务调度业务实现，指派、自抢、弃单与回收统一校验公司归属、车辆状态与一车一单占用。
 */
@Service
public class TaskDispatchServiceImpl implements TaskDispatchService {

    private final CurrentAccountService currentAccountService;
    private final TaskQueryService taskQueryService;
    private final OrderQueryService orderQueryService;
    private final NotificationService notificationService;
    private final ClockService clockService;
    private final AccountMapper accountMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final LogisticsMapper logisticsMapper;
    private final ShipperMapper shipperMapper;
    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final TaskMapper taskMapper;
    private final TaskGoodsMapper taskGoodsMapper;
    private final TaskEventMapper taskEventMapper;

    public TaskDispatchServiceImpl(CurrentAccountService currentAccountService,
                                   TaskQueryService taskQueryService, OrderQueryService orderQueryService,
                                   NotificationService notificationService, ClockService clockService,
                                   AccountMapper accountMapper, DriverMapper driverMapper,
                                   VehicleMapper vehicleMapper, LogisticsMapper logisticsMapper,
                                   ShipperMapper shipperMapper, OrderMapper orderMapper,
                                   OrderGoodsMapper orderGoodsMapper, OrderAddressMapper orderAddressMapper,
                                   TaskMapper taskMapper, TaskGoodsMapper taskGoodsMapper,
                                   TaskEventMapper taskEventMapper) {
        this.currentAccountService = currentAccountService;
        this.taskQueryService = taskQueryService;
        this.orderQueryService = orderQueryService;
        this.notificationService = notificationService;
        this.clockService = clockService;
        this.accountMapper = accountMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.logisticsMapper = logisticsMapper;
        this.shipperMapper = shipperMapper;
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.taskMapper = taskMapper;
        this.taskGoodsMapper = taskGoodsMapper;
        this.taskEventMapper = taskEventMapper;
    }

    /**
     * 指派任务给本公司司机及其绑定车辆。
     *
     * @param taskId 任务 ID
     * @param dto 司机与车辆参数
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO dispatch(Long taskId, TaskDispatchDTO dto) {
        Logistics logistics = currentLogistics();
        Task task = taskQueryService.getLogisticsTask(taskId, logistics.getAccountId());
        if (!TaskStatusEnum.PENDING_ASSIGN.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务当前状态不可指派");
        }
        Driver driver = driverMapper.selectById(dto.getDriverId());
        if (driver == null || !driver.getLogisticsId().equals(logistics.getId())) {
            throw new BusinessException("司机不属于本公司");
        }
        if (!EmploymentStatusEnum.ACTIVE.getCode().equals(driver.getEmploymentStatus())) {
            throw new BusinessException("司机不在职");
        }
        Account driverAccount = accountMapper.selectById(driver.getAccountId());
        if (driverAccount == null || driverAccount.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("司机账号不可用");
        }
        Vehicle vehicle = vehicleMapper.selectById(dto.getVehicleId());
        if (vehicle == null || !vehicle.getLogisticsId().equals(logistics.getId())) {
            throw new BusinessException("车辆不属于本公司");
        }
        if (!VehicleStatusEnum.NORMAL.getCode().equals(vehicle.getVehicleStatus())) {
            throw new BusinessException("车辆当前不可用");
        }
        if (driver.getVehicleId() == null || !driver.getVehicleId().equals(vehicle.getId())) {
            throw new BusinessException("车辆不是该司机绑定的车辆");
        }
        ensureNoTransport(driver.getAccountId(), vehicle.getId());

        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getDriverAccountId, driver.getAccountId())
                .set(Task::getDriverId, driver.getId())
                .set(Task::getVehicleId, vehicle.getId())
                .set(Task::getAssignType, AssignTypeEnum.ASSIGNED.getCode())
                .set(Task::getTaskStatus, TaskStatusEnum.WAIT_EXECUTE.getCode())
                .set(Task::getDriverAck, 0)
                .set(Task::getDriverAckTime, null)
                .set(Task::getOpenClaim, 0)
                .eq(Task::getId, task.getId())
                .eq(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode())
                .isNull(Task::getDriverAccountId));
        if (updated == 0) {
            throw new BusinessException("任务已被指派或领取，请刷新后重试");
        }
        writeTaskEvent(task, TaskEventTypeEnum.DISPATCH, logistics.getAccountId(), null,
                "指派司机" + driver.getName() + "，车辆" + vehicle.getPlateNo());
        notificationService.send(driver.getAccountId(), NoticeTypeEnum.TASK_DISPATCHED, "任务已指派",
                "任务" + task.getTaskNo() + "已指派给您，车辆" + vehicle.getPlateNo(), task.getOrderId(),
                task.getId());
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 开放任务到本公司任务池。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO openClaim(Long taskId) {
        Logistics logistics = currentLogistics();
        Task task = taskQueryService.getLogisticsTask(taskId, logistics.getAccountId());
        if (!TaskStatusEnum.PENDING_ASSIGN.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务当前状态不可开放自抢");
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getOpenClaim, 1)
                .eq(Task::getId, task.getId())
                .eq(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode())
                .isNull(Task::getDriverAccountId));
        if (updated == 0) {
            throw new BusinessException("任务已被指派或领取，请刷新后重试");
        }
        writeTaskEvent(task, TaskEventTypeEnum.OPEN_CLAIM, logistics.getAccountId(), null, "开放到公司任务池");
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 分页查询本公司开放自抢任务概要。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 任务池公开分页数据
     */
    @Override
    public PageResult<DriverTaskPoolVO> pageTaskPool(Integer pageNum, Integer pageSize) {
        Driver driver = currentAccountService.getDriver();
        Logistics logistics = requireDriverLogistics(driver);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        Page<Task> page = taskMapper.selectPage(new Page<>(currentPage, currentSize),
                new LambdaQueryWrapper<Task>()
                        .eq(Task::getLogisticsAccountId, logistics.getAccountId())
                        .eq(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode())
                        .eq(Task::getOpenClaim, 1)
                        .orderByAsc(Task::getId));
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), currentPage, currentSize);
        }
        List<Long> taskIds = page.getRecords().stream().map(Task::getId).toList();
        List<Long> orderIds = page.getRecords().stream().map(Task::getOrderId).distinct().toList();
        Map<Long, List<OrderGoods>> taskGoodsMap = loadTaskGoods(taskIds);
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(orderIds);
        Map<Long, Order> orderMap = loadOrders(orderIds);
        List<DriverTaskPoolVO> records = page.getRecords().stream()
                .map(task -> toPoolVO(task, taskGoodsMap.get(task.getId()), addressMap.get(task.getOrderId()),
                        orderMap.get(task.getOrderId()), logistics.getName()))
                .toList();
        return new PageResult<>(records, page.getTotal(), currentPage, currentSize);
    }

    /**
     * 司机领取本公司开放自抢任务。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO claim(Long taskId) {
        Driver driver = currentAccountService.getDriver();
        Logistics logistics = requireDriverLogistics(driver);
        Task task = taskQueryService.getTask(taskId);
        if (!task.getLogisticsAccountId().equals(logistics.getAccountId())) {
            throw new BusinessException("任务不属于本公司");
        }
        if (!TaskStatusEnum.PENDING_ASSIGN.getCode().equals(task.getTaskStatus())
                || !Integer.valueOf(1).equals(task.getOpenClaim())) {
            throw new BusinessException("任务已被领取");
        }
        Vehicle vehicle = resolveDriverVehicle(driver);
        ensureNoTransport(driver.getAccountId(), vehicle.getId());

        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getDriverAccountId, driver.getAccountId())
                .set(Task::getDriverId, driver.getId())
                .set(Task::getVehicleId, vehicle.getId())
                .set(Task::getAssignType, AssignTypeEnum.CLAIMED.getCode())
                .set(Task::getTaskStatus, TaskStatusEnum.WAIT_EXECUTE.getCode())
                .set(Task::getOpenClaim, 0)
                .set(Task::getDriverAck, 0)
                .set(Task::getDriverAckTime, null)
                .eq(Task::getId, task.getId())
                .eq(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode())
                .eq(Task::getOpenClaim, 1)
                .isNull(Task::getDriverAccountId));
        if (updated == 0) {
            throw new BusinessException("任务已被领取");
        }
        writeTaskEvent(task, TaskEventTypeEnum.CLAIM, driver.getAccountId(), null,
                "司机领取任务，车辆" + vehicle.getPlateNo());
        notificationService.send(logistics.getAccountId(), NoticeTypeEnum.TASK_CLAIMED, "任务已被领取",
                "司机" + driver.getName() + "已领取任务" + task.getTaskNo(), task.getOrderId(), task.getId());
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 分页查询司机本人任务。
     *
     * @param tab 状态标签
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 司机任务分页数据
     */
    @Override
    public PageResult<DriverTaskListVO> pageDriverTasks(String tab, Integer pageNum, Integer pageSize) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<Task> wrapper = new LambdaQueryWrapper<Task>()
                .eq(Task::getDriverAccountId, account.getId());
        TaskStatusEnum status = resolveTabStatus(tab);
        if (status != null) {
            wrapper.eq(Task::getTaskStatus, status.getCode());
        }
        wrapper.orderByDesc(Task::getId);
        Page<Task> page = taskMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), currentPage, currentSize);
        }
        List<Long> taskIds = page.getRecords().stream().map(Task::getId).toList();
        List<Long> orderIds = page.getRecords().stream().map(Task::getOrderId).distinct().toList();
        Map<Long, List<OrderGoods>> taskGoodsMap = loadTaskGoods(taskIds);
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(orderIds);
        Map<Long, Order> orderMap = loadOrders(orderIds);
        List<DriverTaskListVO> records = page.getRecords().stream()
                .map(task -> toListVO(task, taskGoodsMap.get(task.getId()), addressMap.get(task.getOrderId()),
                        orderMap.get(task.getOrderId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), currentPage, currentSize);
    }

    /**
     * 查询司机本人任务详情。
     *
     * @param taskId 任务 ID
     * @return 任务详情
     */
    @Override
    public DriverTaskDetailVO getDriverTaskDetail(Long taskId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        Order order = orderQueryService.getOrder(task.getOrderId());
        DriverTaskDetailVO vo = new DriverTaskDetailVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setOrderId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setAssignType(task.getAssignType());
        vo.setOpenClaim(task.getOpenClaim());
        vo.setDriverAck(task.getDriverAck());
        vo.setDriverAckTime(task.getDriverAckTime());
        vo.setGoods(toTaskGoods(loadTaskGoods(List.of(task.getId())).getOrDefault(task.getId(),
                Collections.emptyList())));
        fillAddresses(vo, orderQueryService.listAddresses(order.getId()));
        fillParties(vo, order);
        if (task.getVehicleId() != null) {
            Vehicle vehicle = vehicleMapper.selectById(task.getVehicleId());
            vo.setVehiclePlateNo(vehicle == null ? null : vehicle.getPlateNo());
        }
        return vo;
    }

    /**
     * 司机确认知悉指派。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO ack(Long taskId) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        if (!TaskStatusEnum.WAIT_EXECUTE.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务当前状态不可确认");
        }
        if (Integer.valueOf(1).equals(task.getDriverAck())) {
            return toBrief(task);
        }
        int updated = taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getDriverAck, 1)
                .set(Task::getDriverAckTime, clockService.now())
                .eq(Task::getId, task.getId())
                .eq(Task::getTaskStatus, TaskStatusEnum.WAIT_EXECUTE.getCode())
                .eq(Task::getDriverAck, 0));
        if (updated > 0) {
            writeTaskEvent(task, TaskEventTypeEnum.ACK, account.getId(), null, "司机确认知悉");
        }
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 司机弃单。
     *
     * @param taskId 任务 ID
     * @param reason 弃单原因
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO abandon(Long taskId, String reason) {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        Task task = taskQueryService.getDriverTask(taskId, account.getId());
        if (!TaskStatusEnum.WAIT_EXECUTE.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(departedStatusMessage(task.getTaskStatus(), "弃单"));
        }
        int updated = clearAssignment(task, TaskStatusEnum.WAIT_EXECUTE);
        if (updated == 0) {
            throw new BusinessException("任务状态已变更，请刷新后重试");
        }
        writeTaskEvent(task, TaskEventTypeEnum.ABANDON, account.getId(), reason.trim(), "司机弃单");
        notificationService.send(task.getLogisticsAccountId(), NoticeTypeEnum.TASK_ABANDONED, "任务已弃单",
                "任务" + task.getTaskNo() + "被司机弃单，原因：" + reason.trim(), task.getOrderId(), task.getId());
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 物流公司强制回收任务。
     *
     * @param taskId 任务 ID
     * @param reason 回收原因
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO reclaim(Long taskId, String reason) {
        Logistics logistics = currentLogistics();
        Task task = taskQueryService.getLogisticsTask(taskId, logistics.getAccountId());
        if (!TaskStatusEnum.WAIT_EXECUTE.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException(departedStatusMessage(task.getTaskStatus(), "回收"));
        }
        Long originalDriverAccountId = task.getDriverAccountId();
        int updated = clearAssignment(task, TaskStatusEnum.WAIT_EXECUTE);
        if (updated == 0) {
            throw new BusinessException("任务状态已变更，请刷新后重试");
        }
        writeTaskEvent(task, TaskEventTypeEnum.RECLAIM, logistics.getAccountId(), reason.trim(), "物流公司强制回收");
        notificationService.send(originalDriverAccountId, NoticeTypeEnum.TASK_RECLAIMED, "任务已被回收",
                "任务" + task.getTaskNo() + "已被公司回收，原因：" + reason.trim(), task.getOrderId(),
                task.getId());
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 调整未出发任务承载的货物。
     *
     * @param taskId 任务 ID
     * @param dto 货物行参数
     * @return 任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO adjustGoods(Long taskId, GoodsIdsDTO dto) {
        Logistics logistics = currentLogistics();
        Task task = taskQueryService.getLogisticsTask(taskId, logistics.getAccountId());
        if (!TaskStatusEnum.PENDING_ASSIGN.getCode().equals(task.getTaskStatus())
                && !TaskStatusEnum.WAIT_EXECUTE.getCode().equals(task.getTaskStatus())) {
            throw new BusinessException("任务已出发，不能调整货物");
        }
        Order order = orderQueryService.getLogisticsOrder(task.getOrderId(), logistics.getAccountId());
        List<Long> goodsIds = dto.getGoodsIds().stream().filter(id -> id != null)
                .collect(Collectors.toCollection(LinkedHashSet::new)).stream().toList();
        if (goodsIds.isEmpty()) {
            throw new BusinessException("请选择至少一行货物");
        }
        Map<Long, OrderGoods> orderGoodsMap = orderQueryService.listGoods(order.getId()).stream()
                .collect(Collectors.toMap(OrderGoods::getId, goods -> goods, (first, second) -> first));
        for (Long goodsId : goodsIds) {
            if (!orderGoodsMap.containsKey(goodsId)) {
                throw new BusinessException("货物不属于该订单");
            }
        }
        List<TaskGoods> occupied = taskGoodsMapper.selectList(new LambdaQueryWrapper<TaskGoods>()
                .in(TaskGoods::getOrderGoodsId, goodsIds)
                .ne(TaskGoods::getTaskId, task.getId()));
        if (!occupied.isEmpty()) {
            throw new BusinessException("货物已被其他任务占用");
        }

        taskGoodsMapper.delete(new LambdaQueryWrapper<TaskGoods>().eq(TaskGoods::getTaskId, task.getId()));
        LocalDateTime now = clockService.now();
        try {
            for (Long goodsId : goodsIds) {
                TaskGoods taskGoods = new TaskGoods();
                taskGoods.setTaskId(task.getId());
                taskGoods.setOrderId(order.getId());
                taskGoods.setOrderGoodsId(goodsId);
                taskGoods.setCreateTime(now);
                taskGoodsMapper.insert(taskGoods);
            }
        } catch (DuplicateKeyException exception) {
            throw new BusinessException("货物已被其他任务占用");
        }
        writeTaskEvent(task, TaskEventTypeEnum.GOODS_ADJUST, logistics.getAccountId(), null,
                "货物行：" + String.join("、", goodsIds.stream()
                        .map(id -> orderGoodsMap.get(id).getGoodsName()).toList()));
        if (task.getDriverAccountId() != null) {
            notificationService.send(task.getDriverAccountId(), NoticeTypeEnum.TASK_GOODS_ADJUSTED,
                    "任务货物已调整", "任务" + task.getTaskNo() + "承载货物已调整，请知悉", task.getOrderId(),
                    task.getId());
        }
        return toBrief(taskMapper.selectById(task.getId()));
    }

    /**
     * 获取当前物流公司资料。
     *
     * @return 物流公司资料
     */
    private Logistics currentLogistics() {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, account.getId()));
        if (logistics == null) {
            throw new BusinessException("物流公司资料不存在");
        }
        return logistics;
    }

    /**
     * 获取司机所属物流公司资料。
     *
     * @param driver 司机资料
     * @return 物流公司资料
     */
    private Logistics requireDriverLogistics(Driver driver) {
        Logistics logistics = logisticsMapper.selectById(driver.getLogisticsId());
        if (logistics == null) {
            throw new BusinessException("物流公司资料不存在");
        }
        return logistics;
    }

    /**
     * 校验司机绑定车辆可用。
     *
     * @param driver 司机资料
     * @return 绑定车辆
     */
    private Vehicle resolveDriverVehicle(Driver driver) {
        if (driver.getVehicleId() == null) {
            throw new BusinessException("司机未绑定车辆");
        }
        Vehicle vehicle = vehicleMapper.selectById(driver.getVehicleId());
        if (vehicle == null || !VehicleStatusEnum.NORMAL.getCode().equals(vehicle.getVehicleStatus())) {
            throw new BusinessException("车辆当前不可用");
        }
        return vehicle;
    }

    /**
     * 校验司机与车辆当前没有运输中任务。
     *
     * @param driverAccountId 司机账号 ID
     * @param vehicleId 车辆 ID
     */
    private void ensureNoTransport(Long driverAccountId, Long vehicleId) {
        long count = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                .eq(Task::getTaskStatus, TaskStatusEnum.IN_TRANSPORT.getCode())
                .and(wrapper -> wrapper.eq(Task::getDriverAccountId, driverAccountId)
                        .or().eq(Task::getVehicleId, vehicleId)));
        if (count > 0) {
            throw new BusinessException("司机或车辆已有运输中任务");
        }
    }

    /**
     * 清空任务归属并回到待指派，仅当任务仍处于指定状态时生效。
     *
     * @param task 任务实体
     * @param expectedStatus 期望当前状态
     * @return 更新行数
     */
    private int clearAssignment(Task task, TaskStatusEnum expectedStatus) {
        return taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getDriverAccountId, null)
                .set(Task::getDriverId, null)
                .set(Task::getVehicleId, null)
                .set(Task::getAssignType, null)
                .set(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode())
                .set(Task::getOpenClaim, 0)
                .set(Task::getDriverAck, 0)
                .set(Task::getDriverAckTime, null)
                .eq(Task::getId, task.getId())
                .eq(Task::getTaskStatus, expectedStatus.getCode()));
    }

    /**
     * 按当前状态给出弃单或回收的拒绝原因。
     *
     * @param taskStatus 任务状态
     * @param actionName 动作名称
     * @return 提示信息
     */
    private String departedStatusMessage(String taskStatus, String actionName) {
        if (TaskStatusEnum.IN_TRANSPORT.getCode().equals(taskStatus)
                || TaskStatusEnum.COMPLETED.getCode().equals(taskStatus)
                || TaskStatusEnum.TERMINATED.getCode().equals(taskStatus)) {
            return "任务已出发，不能" + actionName;
        }
        return "任务当前状态不可" + actionName;
    }

    /**
     * 解析司机任务标签。
     *
     * @param tab 标签
     * @return 对应状态，空标签返回 null
     */
    private TaskStatusEnum resolveTabStatus(String tab) {
        if (tab == null || tab.isBlank()) {
            return null;
        }
        return switch (tab.trim()) {
            case "pending" -> TaskStatusEnum.WAIT_EXECUTE;
            case "in_transport" -> TaskStatusEnum.IN_TRANSPORT;
            case "completed" -> TaskStatusEnum.COMPLETED;
            case "terminated" -> TaskStatusEnum.TERMINATED;
            default -> throw new BusinessException("任务标签不正确");
        };
    }

    /**
     * 批量加载任务承载货物。
     *
     * @param taskIds 任务 ID 列表
     * @return 任务 ID 到货物列表映射
     */
    private Map<Long, List<OrderGoods>> loadTaskGoods(List<Long> taskIds) {
        if (taskIds.isEmpty()) {
            return Collections.emptyMap();
        }
        List<TaskGoods> taskGoods = taskGoodsMapper.selectList(new LambdaQueryWrapper<TaskGoods>()
                .in(TaskGoods::getTaskId, taskIds));
        if (taskGoods.isEmpty()) {
            return Collections.emptyMap();
        }
        List<Long> goodsIds = taskGoods.stream().map(TaskGoods::getOrderGoodsId).distinct().toList();
        Map<Long, OrderGoods> goodsMap = orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .in(OrderGoods::getId, goodsIds))
                .stream().collect(Collectors.toMap(OrderGoods::getId, item -> item, (first, second) -> first));
        Map<Long, List<OrderGoods>> result = new HashMap<>();
        for (TaskGoods item : taskGoods) {
            OrderGoods goods = goodsMap.get(item.getOrderGoodsId());
            if (goods != null) {
                result.computeIfAbsent(item.getTaskId(), key -> new ArrayList<>()).add(goods);
            }
        }
        return result;
    }

    /**
     * 批量加载订单地址。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到地址列表映射
     */
    private Map<Long, List<OrderAddress>> loadAddressMap(List<Long> orderIds) {
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                        .in(OrderAddress::getOrderId, orderIds)
                        .orderByAsc(OrderAddress::getId))
                .stream().collect(Collectors.groupingBy(OrderAddress::getOrderId));
    }

    /**
     * 批量加载订单。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到订单映射
     */
    private Map<Long, Order> loadOrders(List<Long> orderIds) {
        return orderMapper.selectList(new LambdaQueryWrapper<Order>().in(Order::getId, orderIds))
                .stream().collect(Collectors.toMap(Order::getId, order -> order, (first, second) -> first));
    }

    /**
     * 构造任务池公开概要。
     *
     * @param task 任务实体
     * @param goods 承载货物
     * @param addresses 地址
     * @param order 订单实体
     * @param logisticsName 物流公司名称
     * @return 任务池概要
     */
    private DriverTaskPoolVO toPoolVO(Task task, List<OrderGoods> goods, List<OrderAddress> addresses,
                                      Order order, String logisticsName) {
        DriverTaskPoolVO vo = new DriverTaskPoolVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setLogisticsName(logisticsName);
        if (goods != null && !goods.isEmpty()) {
            OrderGoods first = goods.get(0);
            vo.setGoodsName(first.getGoodsName());
            vo.setGoodsType(first.getGoodsType());
            vo.setQuantity(first.getQuantity());
            vo.setUnit(first.getUnit());
        }
        if (addresses != null) {
            for (OrderAddress address : addresses) {
                if (AddressTypeEnum.PICKUP.getCode().equals(address.getAddressType())) {
                    vo.setPickupProvince(address.getProvince());
                    vo.setPickupCity(address.getCity());
                    vo.setPickupDistrict(address.getDistrict());
                } else if (AddressTypeEnum.DELIVERY.getCode().equals(address.getAddressType())) {
                    vo.setDeliveryProvince(address.getProvince());
                    vo.setDeliveryCity(address.getCity());
                    vo.setDeliveryDistrict(address.getDistrict());
                }
            }
        }
        if (order != null) {
            vo.setPickupTime(order.getPickupTime());
            vo.setDeliveryTime(order.getDeliveryTime());
        }
        return vo;
    }

    /**
     * 构造司机任务列表概要。
     *
     * @param task 任务实体
     * @param goods 承载货物
     * @param addresses 地址
     * @param order 订单实体
     * @return 任务列表概要
     */
    private DriverTaskListVO toListVO(Task task, List<OrderGoods> goods, List<OrderAddress> addresses,
                                      Order order) {
        DriverTaskListVO vo = new DriverTaskListVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setOrderId(task.getOrderId());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setAssignType(task.getAssignType());
        vo.setOpenClaim(task.getOpenClaim());
        vo.setDriverAck(task.getDriverAck());
        vo.setGoods(toTaskGoods(goods == null ? Collections.emptyList() : goods));
        if (order != null) {
            vo.setOrderNo(order.getOrderNo());
            vo.setWaybillNo(order.getWaybillNo());
            vo.setPickupTime(order.getPickupTime());
            vo.setDeliveryTime(order.getDeliveryTime());
        }
        if (addresses != null) {
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
        return vo;
    }

    /**
     * 将订单货物转换为任务货物展示项。
     *
     * @param goods 货物列表
     * @return 任务货物列表
     */
    private List<TaskGoodsItemVO> toTaskGoods(List<OrderGoods> goods) {
        List<TaskGoodsItemVO> result = new ArrayList<>();
        for (OrderGoods item : goods) {
            TaskGoodsItemVO vo = new TaskGoodsItemVO();
            vo.setId(item.getId());
            vo.setGoodsName(item.getGoodsName());
            vo.setGoodsType(item.getGoodsType());
            vo.setQuantity(item.getQuantity());
            vo.setWeight(item.getWeight());
            vo.setVolume(item.getVolume());
            vo.setUnit(item.getUnit());
            result.add(vo);
        }
        return result;
    }

    /**
     * 填充司机任务详情的装卸货完整地址。
     *
     * @param vo 任务详情
     * @param addresses 地址列表
     */
    private void fillAddresses(DriverTaskDetailVO vo, List<OrderAddress> addresses) {
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
     * 填充司机任务详情的货主与物流公司信息。
     *
     * @param vo 任务详情
     * @param order 订单实体
     */
    private void fillParties(DriverTaskDetailVO vo, Order order) {
        Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                .eq(Shipper::getAccountId, order.getShipperAccountId()));
        if (shipper != null) {
            vo.setShipperName(shipper.getName());
        }
        if (order.getLogisticsAccountId() == null) {
            return;
        }
        Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, order.getLogisticsAccountId()));
        if (logistics != null) {
            vo.setLogisticsName(logistics.getName());
            vo.setLogisticsContactPhone(logistics.getPhone());
        }
        Onboarding onboarding = orderQueryService.getApprovedOnboarding(order.getLogisticsAccountId());
        if (onboarding != null) {
            vo.setLogisticsContactName(onboarding.getContactName());
            if (vo.getLogisticsContactPhone() == null) {
                vo.setLogisticsContactPhone(onboarding.getContactPhone());
            }
        }
    }

    /**
     * 写入任务事件留痕。
     *
     * @param task 任务实体
     * @param eventType 事件类型
     * @param operatorAccountId 操作账号 ID
     * @param reason 原因
     * @param detail 事件详情
     */
    private void writeTaskEvent(Task task, TaskEventTypeEnum eventType, Long operatorAccountId,
                                String reason, String detail) {
        TaskEvent event = new TaskEvent();
        event.setTaskId(task.getId());
        event.setOrderId(task.getOrderId());
        event.setEventType(eventType.getCode());
        event.setOperatorAccountId(operatorAccountId);
        event.setReason(reason);
        event.setDetail(detail);
        event.setCreateTime(clockService.now());
        taskEventMapper.insert(event);
    }

    /**
     * 将任务实体转换为操作结果概要。
     *
     * @param task 任务实体
     * @return 任务概要
     */
    private TaskBriefVO toBrief(Task task) {
        TaskBriefVO vo = new TaskBriefVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setAssignType(task.getAssignType());
        vo.setOpenClaim(task.getOpenClaim());
        vo.setDriverAck(task.getDriverAck());
        return vo;
    }
}
