package com.example.web.module.order.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.web.exception.BusinessException;
import com.example.web.exception.ForbiddenException;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.onboarding.enums.AuditStatusEnum;
import com.example.web.module.onboarding.mapper.OnboardingMapper;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.entity.OrderAddress;
import com.example.web.module.order.entity.OrderGoods;
import com.example.web.module.order.mapper.OrderAddressMapper;
import com.example.web.module.order.mapper.OrderGoodsMapper;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.vo.OrderTaskSummaryVO;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

/**
 * 订单查询公共服务，供货主详情、订单池、运单与监管等后续票据复用数据隔离与任务汇总能力。
 */
@Service
public class OrderQueryService {

    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final TaskMapper taskMapper;
    private final DriverMapper driverMapper;
    private final VehicleMapper vehicleMapper;
    private final OnboardingMapper onboardingMapper;

    /**
     * 注入订单、货物、地址、任务与资质的查询依赖。
     *
     * @param orderMapper 订单数据访问
     * @param orderGoodsMapper 货物明细数据访问
     * @param orderAddressMapper 地址数据访问
     * @param taskMapper 任务数据访问
     * @param driverMapper 司机数据访问
     * @param vehicleMapper 车辆数据访问
     * @param onboardingMapper 入驻数据访问
     */
    public OrderQueryService(OrderMapper orderMapper, OrderGoodsMapper orderGoodsMapper,
                             OrderAddressMapper orderAddressMapper, TaskMapper taskMapper,
                             DriverMapper driverMapper, VehicleMapper vehicleMapper,
                             OnboardingMapper onboardingMapper) {
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.taskMapper = taskMapper;
        this.driverMapper = driverMapper;
        this.vehicleMapper = vehicleMapper;
        this.onboardingMapper = onboardingMapper;
    }

    /**
     * 查询订单原始实体。
     *
     * @param orderId 订单 ID
     * @return 订单实体，不存在时抛出业务异常
     */
    public Order getOrder(Long orderId) {
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        return order;
    }

    /**
     * 查询货主本人订单，其他货主访问时抛出无权限异常。
     *
     * @param orderId 订单 ID
     * @param shipperAccountId 当前货主账号 ID
     * @return 订单实体
     */
    public Order getShipperOrder(Long orderId, Long shipperAccountId) {
        Order order = getOrder(orderId);
        if (!order.getShipperAccountId().equals(shipperAccountId)) {
            throw new ForbiddenException("无权访问该订单");
        }
        return order;
    }

    /**
     * 查询物流公司名下订单，供运单详情、取消与监管票据复用。
     *
     * @param orderId 订单 ID
     * @param logisticsAccountId 当前物流公司账号 ID
     * @return 订单实体
     */
    public Order getLogisticsOrder(Long orderId, Long logisticsAccountId) {
        Order order = getOrder(orderId);
        if (order.getLogisticsAccountId() == null
                || !order.getLogisticsAccountId().equals(logisticsAccountId)) {
            throw new ForbiddenException("无权访问该订单");
        }
        return order;
    }

    /**
     * 查询订单货物明细，按 ID 升序返回。
     *
     * @param orderId 订单 ID
     * @return 货物明细列表
     */
    public List<OrderGoods> listGoods(Long orderId) {
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                .eq(OrderGoods::getOrderId, orderId)
                .orderByAsc(OrderGoods::getId));
    }

    /**
     * 查询订单装卸货点，按 ID 升序返回。
     *
     * @param orderId 订单 ID
     * @return 地址列表
     */
    public List<OrderAddress> listAddresses(Long orderId) {
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                .eq(OrderAddress::getOrderId, orderId)
                .orderByAsc(OrderAddress::getId));
    }

    /**
     * 查询订单任务概要，司机姓名与车牌在任务已指派时返回。
     *
     * @param orderId 订单 ID
     * @return 任务概要列表
     */
    public List<OrderTaskSummaryVO> listTaskSummaries(Long orderId) {
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getOrderId, orderId)
                .orderByAsc(Task::getId));
        if (tasks.isEmpty()) {
            return Collections.emptyList();
        }
        Map<Long, String> driverNames = loadDriverNames(tasks);
        Map<Long, String> plateNos = loadVehiclePlateNos(tasks);
        return tasks.stream().map(task -> toTaskSummary(task, driverNames, plateNos)).toList();
    }

    /**
     * 查询账号最近一次已通过的入驻申请，用于接单后展示物流公司联系人。
     *
     * @param accountId 账号 ID
     * @return 最近一次已通过入驻申请，不存在时返回 null
     */
    public Onboarding getApprovedOnboarding(Long accountId) {
        List<Onboarding> list = onboardingMapper.selectList(new LambdaQueryWrapper<Onboarding>()
                .eq(Onboarding::getAccountId, accountId)
                .eq(Onboarding::getAuditStatus, AuditStatusEnum.APPROVED.getCode())
                .orderByDesc(Onboarding::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 批量加载任务执行司机姓名。
     *
     * @param tasks 任务列表
     * @return 司机账号 ID 到姓名映射
     */
    private Map<Long, String> loadDriverNames(List<Task> tasks) {
        List<Long> driverAccountIds = tasks.stream().map(Task::getDriverAccountId)
                .filter(accountId -> accountId != null).distinct().toList();
        if (driverAccountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return driverMapper.selectList(new LambdaQueryWrapper<Driver>()
                        .in(Driver::getAccountId, driverAccountIds))
                .stream().collect(Collectors.toMap(Driver::getAccountId, Driver::getName, (first, second) -> first));
    }

    /**
     * 批量加载任务执行车辆车牌号。
     *
     * @param tasks 任务列表
     * @return 车辆 ID 到车牌号映射
     */
    private Map<Long, String> loadVehiclePlateNos(List<Task> tasks) {
        List<Long> vehicleIds = tasks.stream().map(Task::getVehicleId)
                .filter(vehicleId -> vehicleId != null).distinct().toList();
        if (vehicleIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return vehicleMapper.selectBatchIds(vehicleIds).stream()
                .collect(Collectors.toMap(Vehicle::getId, Vehicle::getPlateNo, (first, second) -> first));
    }

    /**
     * 将任务实体转换为概要 VO。
     *
     * @param task 任务实体
     * @param driverNames 司机姓名映射
     * @param plateNos 车辆车牌映射
     * @return 任务概要
     */
    private OrderTaskSummaryVO toTaskSummary(Task task, Map<Long, String> driverNames,
                                             Map<Long, String> plateNos) {
        OrderTaskSummaryVO vo = new OrderTaskSummaryVO();
        vo.setTaskId(task.getId());
        vo.setTaskNo(task.getTaskNo());
        vo.setTaskStatus(task.getTaskStatus());
        vo.setDriverName(task.getDriverAccountId() == null ? null : driverNames.get(task.getDriverAccountId()));
        vo.setVehiclePlateNo(task.getVehicleId() == null ? null : plateNos.get(task.getVehicleId()));
        return vo;
    }
}
