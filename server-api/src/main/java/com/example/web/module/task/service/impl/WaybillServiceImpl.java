package com.example.web.module.task.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.BusinessNoGenerator;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.checkin.vo.TaskCheckinSummaryVO;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
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
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.entity.TaskEvent;
import com.example.web.module.task.entity.TaskGoods;
import com.example.web.module.task.enums.TaskEventTypeEnum;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskEventMapper;
import com.example.web.module.task.mapper.TaskGoodsMapper;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.task.service.TaskQueryService;
import com.example.web.module.task.service.WaybillService;
import com.example.web.module.task.vo.TaskBriefVO;
import com.example.web.module.task.vo.TaskGoodsItemVO;
import com.example.web.module.task.vo.WaybillDetailVO;
import com.example.web.module.task.vo.WaybillGoodsVO;
import com.example.web.module.task.vo.WaybillListVO;
import com.example.web.module.task.vo.WaybillProgressVO;
import com.example.web.module.task.vo.WaybillTaskVO;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.security.CurrentAccountService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 运单查询与拆分业务实现，按整行货物创建任务并保证同一明细行只归属一个任务。
 */
@Service
public class WaybillServiceImpl implements WaybillService {

    private final CurrentAccountService currentAccountService;
    private final OrderQueryService orderQueryService;
    private final TaskQueryService taskQueryService;
    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final TaskMapper taskMapper;
    private final TaskGoodsMapper taskGoodsMapper;
    private final TaskEventMapper taskEventMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final ShipperMapper shipperMapper;
    private final BusinessNoGenerator businessNoGenerator;
    private final ClockService clockService;

    public WaybillServiceImpl(CurrentAccountService currentAccountService, OrderQueryService orderQueryService,
                              TaskQueryService taskQueryService, OrderMapper orderMapper,
                              OrderGoodsMapper orderGoodsMapper, OrderAddressMapper orderAddressMapper,
                              TaskMapper taskMapper, TaskGoodsMapper taskGoodsMapper,
                              TaskEventMapper taskEventMapper, DriverMapper driverMapper,
                              VehicleMapper vehicleMapper,
                              ShipperMapper shipperMapper,
                              BusinessNoGenerator businessNoGenerator,
                              ClockService clockService) {
        this.currentAccountService = currentAccountService;
        this.orderQueryService = orderQueryService;
        this.taskQueryService = taskQueryService;
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.taskMapper = taskMapper;
        this.taskGoodsMapper = taskGoodsMapper;
        this.taskEventMapper = taskEventMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.shipperMapper = shipperMapper;
        this.businessNoGenerator = businessNoGenerator;
        this.clockService = clockService;
    }

    /**
     * 分页查询本公司已接运单。
     *
     * @param orderStatus 订单状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 运单列表分页数据
     */
    @Override
    public PageResult<WaybillListVO> pageWaybills(String orderStatus, Integer pageNum, Integer pageSize) {
        Account account = requireLogisticsAccount();
        int currentPage = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int currentSize = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getLogisticsAccountId, account.getId());
        if (orderStatus != null && !orderStatus.isBlank()) {
            OrderStatusEnum status = OrderStatusEnum.of(orderStatus.trim());
            if (status == null) {
                throw new BusinessException("订单状态不正确");
            }
            wrapper.eq(Order::getOrderStatus, status.getCode());
        }
        wrapper.orderByDesc(Order::getAcceptTime).orderByDesc(Order::getId);
        Page<Order> page = orderMapper.selectPage(new Page<>(currentPage, currentSize), wrapper);
        if (page.getRecords().isEmpty()) {
            return new PageResult<>(Collections.emptyList(), page.getTotal(), currentPage, currentSize);
        }
        List<Long> orderIds = page.getRecords().stream().map(Order::getId).toList();
        Map<Long, List<OrderGoods>> goodsMap = loadGoodsMap(orderIds);
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(orderIds);
        Map<Long, Integer> assignedMap = countAssignedGoods(orderIds);
        Map<Long, Integer> taskCountMap = countTasks(orderIds);
        List<WaybillListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order, goodsMap.get(order.getId()), addressMap.get(order.getId()),
                        assignedMap.getOrDefault(order.getId(), 0), taskCountMap.getOrDefault(order.getId(), 0)))
                .toList();
        return new PageResult<>(records, page.getTotal(), currentPage, currentSize);
    }

    /**
     * 查询本公司运单详情。
     *
     * @param orderId 订单 ID
     * @return 运单详情
     */
    @Override
    public WaybillDetailVO getWaybillDetail(Long orderId) {
        Account account = requireLogisticsAccount();
        Order order = orderQueryService.getLogisticsOrder(orderId, account.getId());
        List<OrderGoods> goods = orderQueryService.listGoods(order.getId());
        List<OrderAddress> addresses = orderQueryService.listAddresses(order.getId());
        List<Task> tasks = listOrderTasks(order.getId());
        List<TaskGoods> taskGoods = taskGoodsMapper.selectList(new LambdaQueryWrapper<TaskGoods>()
                .eq(TaskGoods::getOrderId, order.getId()));
        Map<Long, Task> taskMap = tasks.stream()
                .collect(Collectors.toMap(Task::getId, task -> task, (first, second) -> first));
        Map<Long, TaskGoods> assignedByGoods = taskGoods.stream()
                .collect(Collectors.toMap(TaskGoods::getOrderGoodsId, item -> item, (first, second) -> first));
        Map<Long, List<OrderGoods>> taskGoodsMap = groupTaskGoods(taskGoods, goods);
        Map<Long, String> driverNames = loadDriverNames(tasks);
        Map<Long, String> driverPhones = loadDriverPhones(tasks);
        Map<Long, String> plateNos = loadVehiclePlateNos(tasks);
        Map<Long, TaskCheckinSummaryVO> summaries = taskQueryService.getCheckinSummaries(tasks.stream()
                .map(Task::getId).toList());

        WaybillDetailVO vo = new WaybillDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setShipperAccountId(order.getShipperAccountId());
        Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                .eq(Shipper::getAccountId, order.getShipperAccountId()));
        if (shipper != null) {
            vo.setShipperName(shipper.getName());
        }
        vo.setFreightAmount(order.getFreightAmount());
        vo.setRemark(order.getRemark());
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setAcceptTime(order.getAcceptTime());
        fillAddresses(vo, addresses);
        vo.setGoods(toWaybillGoods(goods, assignedByGoods, taskMap));
        vo.setTasks(toWaybillTasks(tasks, taskGoodsMap, driverNames, driverPhones, plateNos, summaries));
        vo.setProgress(toProgress(goods, assignedByGoods, tasks, summaries));
        return vo;
    }

    /**
     * 按货物明细整行创建运输任务。
     *
     * @param orderId 订单 ID
     * @param dto 货物行参数
     * @return 新建任务概要
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public TaskBriefVO createTask(Long orderId, GoodsIdsDTO dto) {
        Account account = requireLogisticsAccount();
        Order order = orderQueryService.getLogisticsOrder(orderId, account.getId());
        if (!OrderStatusEnum.ACCEPTED.getCode().equals(order.getOrderStatus())
                && !OrderStatusEnum.DISPATCHING.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("订单当前状态不可拆分任务");
        }
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
                .in(TaskGoods::getOrderGoodsId, goodsIds));
        if (!occupied.isEmpty()) {
            throw new BusinessException("货物已被其他任务占用");
        }

        LocalDateTime now = clockService.now();
        Task task = new Task();
        task.setTaskNo(businessNoGenerator.nextTaskNo());
        task.setOrderId(order.getId());
        task.setLogisticsAccountId(account.getId());
        task.setTaskStatus(TaskStatusEnum.PENDING_ASSIGN.getCode());
        task.setOpenClaim(0);
        task.setDriverAck(0);
        task.setCreateTime(now);
        try {
            taskMapper.insert(task);
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

        orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.DISPATCHING.getCode())
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.ACCEPTED.getCode()));
        writeTaskEvent(task, TaskEventTypeEnum.CREATE, account.getId(), null,
                "货物行：" + String.join("、", goodsIds.stream()
                        .map(id -> orderGoodsMap.get(id).getGoodsName()).toList()));
        return toBriefVO(taskMapper.selectById(task.getId()));
    }

    /**
     * 校验当前账号为物流公司。
     *
     * @return 当前物流公司账号
     */
    private Account requireLogisticsAccount() {
        return currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
    }

    /**
     * 查询订单任务，按创建顺序返回。
     *
     * @param orderId 订单 ID
     * @return 任务列表
     */
    private List<Task> listOrderTasks(Long orderId) {
        return taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getOrderId, orderId)
                .orderByAsc(Task::getId));
    }

    /**
     * 批量加载订单货物。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到货物列表映射
     */
    private Map<Long, List<OrderGoods>> loadGoodsMap(List<Long> orderIds) {
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .in(OrderGoods::getOrderId, orderIds)
                        .orderByAsc(OrderGoods::getId))
                .stream().collect(Collectors.groupingBy(OrderGoods::getOrderId));
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
     * 统计各订单已分配货物行数。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到已分配行数映射
     */
    private Map<Long, Integer> countAssignedGoods(List<Long> orderIds) {
        List<TaskGoods> taskGoods = taskGoodsMapper.selectList(new LambdaQueryWrapper<TaskGoods>()
                .in(TaskGoods::getOrderId, orderIds));
        Map<Long, Integer> counts = new HashMap<>();
        for (TaskGoods item : taskGoods) {
            counts.merge(item.getOrderId(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * 统计各订单任务数。
     *
     * @param orderIds 订单 ID 列表
     * @return 订单 ID 到任务数映射
     */
    private Map<Long, Integer> countTasks(List<Long> orderIds) {
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .in(Task::getOrderId, orderIds));
        Map<Long, Integer> counts = new HashMap<>();
        for (Task task : tasks) {
            counts.merge(task.getOrderId(), 1, Integer::sum);
        }
        return counts;
    }

    /**
     * 将任务货物关联按任务分组并转换为订单货物明细。
     *
     * @param taskGoods 任务货物关联
     * @param goods 订单全部货物
     * @return 任务 ID 到货物列表映射
     */
    private Map<Long, List<OrderGoods>> groupTaskGoods(List<TaskGoods> taskGoods, List<OrderGoods> goods) {
        Map<Long, OrderGoods> goodsMap = goods.stream()
                .collect(Collectors.toMap(OrderGoods::getId, item -> item, (first, second) -> first));
        Map<Long, List<OrderGoods>> result = new HashMap<>();
        for (TaskGoods item : taskGoods) {
            OrderGoods orderGoods = goodsMap.get(item.getOrderGoodsId());
            if (orderGoods != null) {
                result.computeIfAbsent(item.getTaskId(), key -> new ArrayList<>()).add(orderGoods);
            }
        }
        return result;
    }

    /**
     * 批量加载任务司机姓名。
     *
     * @param tasks 任务列表
     * @return 司机账号 ID 到姓名映射
     */
    private Map<Long, String> loadDriverNames(List<Task> tasks) {
        List<Long> accountIds = tasks.stream().map(Task::getDriverAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return driverMapper.selectList(new LambdaQueryWrapper<Driver>().in(Driver::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Driver::getAccountId, Driver::getName,
                        (first, second) -> first));
    }

    /**
     * 批量加载任务司机电话。
     *
     * @param tasks 任务列表
     * @return 司机账号 ID 到电话映射
     */
    private Map<Long, String> loadDriverPhones(List<Task> tasks) {
        List<Long> accountIds = tasks.stream().map(Task::getDriverAccountId)
                .filter(id -> id != null).distinct().toList();
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return driverMapper.selectList(new LambdaQueryWrapper<Driver>().in(Driver::getAccountId, accountIds))
                .stream().collect(Collectors.toMap(Driver::getAccountId, Driver::getPhone,
                        (first, second) -> first));
    }

    /**
     * 批量加载任务车辆车牌。
     *
     * @param tasks 任务列表
     * @return 车辆 ID 到车牌映射
     */
    private Map<Long, String> loadVehiclePlateNos(List<Task> tasks) {
        List<Long> vehicleIds = tasks.stream().map(Task::getVehicleId)
                .filter(id -> id != null).distinct().toList();
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlateNo, (first, second) -> first));
    }

    /**
     * 构造运单列表概要。
     *
     * @param order 订单实体
     * @param goods 货物列表
     * @param addresses 地址列表
     * @param assignedCount 已分配货物行数
     * @param taskCount 任务数
     * @return 运单列表概要
     */
    private WaybillListVO toListVO(Order order, List<OrderGoods> goods, List<OrderAddress> addresses,
                                   Integer assignedCount, Integer taskCount) {
        WaybillListVO vo = new WaybillListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setWaybillNo(order.getWaybillNo());
        vo.setOrderStatus(order.getOrderStatus());
        vo.setFreightAmount(order.getFreightAmount());
        int goodsCount = goods == null ? 0 : goods.size();
        vo.setGoodsCount(goodsCount);
        vo.setAssignedGoodsCount(assignedCount);
        vo.setUnassignedGoodsCount(goodsCount - assignedCount);
        vo.setTaskCount(taskCount);
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setAcceptTime(order.getAcceptTime());
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
     * 填充运单装卸货完整地址。
     *
     * @param vo 运单详情
     * @param addresses 地址列表
     */
    private void fillAddresses(WaybillDetailVO vo, List<OrderAddress> addresses) {
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
     * 构造运单货物行列表并标记分配状态。
     *
     * @param goods 订单货物
     * @param assignedByGoods 货物到任务关联映射
     * @param taskMap 任务 ID 到任务映射
     * @return 运单货物列表
     */
    private List<WaybillGoodsVO> toWaybillGoods(List<OrderGoods> goods, Map<Long, TaskGoods> assignedByGoods,
                                                Map<Long, Task> taskMap) {
        List<WaybillGoodsVO> result = new ArrayList<>();
        for (OrderGoods item : goods) {
            WaybillGoodsVO vo = new WaybillGoodsVO();
            vo.setId(item.getId());
            vo.setGoodsName(item.getGoodsName());
            vo.setGoodsType(item.getGoodsType());
            vo.setQuantity(item.getQuantity());
            vo.setWeight(item.getWeight());
            vo.setVolume(item.getVolume());
            vo.setUnit(item.getUnit());
            vo.setRemark(item.getRemark());
            TaskGoods assigned = assignedByGoods.get(item.getId());
            vo.setAssigned(assigned != null);
            if (assigned != null) {
                vo.setTaskId(assigned.getTaskId());
                Task task = taskMap.get(assigned.getTaskId());
                vo.setTaskNo(task == null ? null : task.getTaskNo());
            }
            result.add(vo);
        }
        return result;
    }

    /**
     * 构造运单任务列表。
     *
     * @param tasks 任务列表
     * @param taskGoodsMap 任务货物映射
     * @param driverNames 司机姓名映射
     * @param driverPhones 司机电话映射
     * @param plateNos 车牌映射
     * @param summaries 打卡汇总映射
     * @return 运单任务列表
     */
    private List<WaybillTaskVO> toWaybillTasks(List<Task> tasks, Map<Long, List<OrderGoods>> taskGoodsMap,
                                               Map<Long, String> driverNames, Map<Long, String> driverPhones,
                                               Map<Long, String> plateNos,
                                               Map<Long, TaskCheckinSummaryVO> summaries) {
        List<WaybillTaskVO> result = new ArrayList<>();
        for (Task task : tasks) {
            WaybillTaskVO vo = new WaybillTaskVO();
            vo.setTaskId(task.getId());
            vo.setTaskNo(task.getTaskNo());
            vo.setTaskStatus(task.getTaskStatus());
            vo.setAssignType(task.getAssignType());
            vo.setOpenClaim(task.getOpenClaim());
            vo.setDriverAck(task.getDriverAck());
            vo.setDriverId(task.getDriverId());
            vo.setDriverAccountId(task.getDriverAccountId());
            vo.setDriverName(task.getDriverAccountId() == null ? null
                    : driverNames.get(task.getDriverAccountId()));
            vo.setDriverPhone(task.getDriverAccountId() == null ? null
                    : driverPhones.get(task.getDriverAccountId()));
            vo.setVehicleId(task.getVehicleId());
            vo.setVehiclePlateNo(task.getVehicleId() == null ? null : plateNos.get(task.getVehicleId()));
            vo.setGoods(toTaskGoods(taskGoodsMap.getOrDefault(task.getId(), Collections.emptyList())));
            TaskCheckinSummaryVO summary = summaries.get(task.getId());
            vo.setDepartCheckins(summary == null ? Collections.emptyList() : summary.getDepartVersions());
            vo.setArriveCheckins(summary == null ? Collections.emptyList() : summary.getArriveVersions());
            result.add(vo);
        }
        return result;
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
     * 构造运单进度。
     *
     * @param goods 订单货物
     * @param assignedByGoods 已分配货物映射
     * @param tasks 任务列表
     * @param summaries 打卡汇总映射
     * @return 运单进度
     */
    private WaybillProgressVO toProgress(List<OrderGoods> goods, Map<Long, TaskGoods> assignedByGoods,
                                         List<Task> tasks, Map<Long, TaskCheckinSummaryVO> summaries) {
        WaybillProgressVO vo = new WaybillProgressVO();
        int goodsTotal = goods.size();
        int assigned = assignedByGoods.size();
        vo.setGoodsTotal(goodsTotal);
        vo.setAssignedGoods(assigned);
        vo.setUnassignedGoods(goodsTotal - assigned);
        vo.setTaskTotal(tasks.size());
        int pendingAssign = 0;
        int waitExecute = 0;
        int inTransport = 0;
        int completed = 0;
        int terminated = 0;
        int departCount = 0;
        int arriveCount = 0;
        for (Task task : tasks) {
            TaskStatusEnum status = TaskStatusEnum.of(task.getTaskStatus());
            if (status == TaskStatusEnum.PENDING_ASSIGN) {
                pendingAssign++;
            } else if (status == TaskStatusEnum.WAIT_EXECUTE) {
                waitExecute++;
            } else if (status == TaskStatusEnum.IN_TRANSPORT) {
                inTransport++;
            } else if (status == TaskStatusEnum.COMPLETED) {
                completed++;
            } else if (status == TaskStatusEnum.TERMINATED) {
                terminated++;
            }
            TaskCheckinSummaryVO summary = summaries.get(task.getId());
            if (summary != null && !summary.getDepartVersions().isEmpty()) {
                departCount++;
            }
            if (summary != null && !summary.getArriveVersions().isEmpty()) {
                arriveCount++;
            }
        }
        vo.setPendingAssignCount(pendingAssign);
        vo.setWaitExecuteCount(waitExecute);
        vo.setInTransportCount(inTransport);
        vo.setCompletedCount(completed);
        vo.setTerminatedCount(terminated);
        vo.setDepartCount(departCount);
        vo.setArriveCount(arriveCount);
        return vo;
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
    private TaskBriefVO toBriefVO(Task task) {
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
