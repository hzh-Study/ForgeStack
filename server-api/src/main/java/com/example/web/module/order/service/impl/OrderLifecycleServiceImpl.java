package com.example.web.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.web.common.time.ClockService;
import com.example.web.exception.BusinessException;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderLifecycleService;
import com.example.web.module.order.service.OrderQueryService;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单生命周期业务实现，货主与物流公司取消共用同一套任务状态校验与通知逻辑。
 */
@Service
public class OrderLifecycleServiceImpl implements OrderLifecycleService {

    private final OrderQueryService orderQueryService;
    private final OrderMapper orderMapper;
    private final TaskMapper taskMapper;
    private final NotificationService notificationService;
    private final ClockService clockService;

    /**
     * 注入订单取消所需的查询、任务与通知依赖。
     *
     * @param orderQueryService 订单查询公共服务
     * @param orderMapper 订单数据访问
     * @param taskMapper 任务数据访问
     * @param notificationService 系统通知服务
     * @param clockService 业务时间服务
     */
    public OrderLifecycleServiceImpl(OrderQueryService orderQueryService, OrderMapper orderMapper,
                                     TaskMapper taskMapper, NotificationService notificationService,
                                     ClockService clockService) {
        this.orderQueryService = orderQueryService;
        this.orderMapper = orderMapper;
        this.taskMapper = taskMapper;
        this.notificationService = notificationService;
        this.clockService = clockService;
    }

    /**
     * 货主取消本人订单，要求订单归属当前货主。
     *
     * @param orderId 订单 ID
     * @param shipperAccountId 货主账号 ID
     * @param reason 取消原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByShipper(Long orderId, Long shipperAccountId, String reason) {
        Order order = orderQueryService.getShipperOrder(orderId, shipperAccountId);
        cancelOrder(order, shipperAccountId, reason);
    }

    /**
     * 物流公司取消本公司订单，供后续运单票据直接复用。
     *
     * @param orderId 订单 ID
     * @param logisticsAccountId 物流公司账号 ID
     * @param reason 取消原因
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void cancelByLogistics(Long orderId, Long logisticsAccountId, String reason) {
        Order order = orderQueryService.getLogisticsOrder(orderId, logisticsAccountId);
        cancelOrder(order, logisticsAccountId, reason);
    }

    /**
     * 执行取消：校验状态与任务是否已出发，终止未出发任务并通知相关方。
     *
     * @param order 订单实体
     * @param operatorAccountId 操作账号 ID
     * @param reason 取消原因
     */
    private void cancelOrder(Order order, Long operatorAccountId, String reason) {
        if (reason == null || reason.isBlank()) {
            throw new BusinessException("取消原因不能为空");
        }
        String currentStatus = order.getOrderStatus();
        if (!OrderStatusEnum.ACCEPTED.getCode().equals(currentStatus)
                && !OrderStatusEnum.DISPATCHING.getCode().equals(currentStatus)) {
            throw new BusinessException("订单当前状态不可取消");
        }
        List<Task> tasks = taskMapper.selectList(new LambdaQueryWrapper<Task>()
                .eq(Task::getOrderId, order.getId()));
        boolean departed = tasks.stream().anyMatch(task ->
                TaskStatusEnum.IN_TRANSPORT.getCode().equals(task.getTaskStatus())
                        || TaskStatusEnum.COMPLETED.getCode().equals(task.getTaskStatus()));
        if (departed) {
            throw new BusinessException("任务已出发，不能取消订单");
        }

        LocalDateTime now = clockService.now();
        String trimmedReason = reason.trim();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getOrderStatus, OrderStatusEnum.CANCELED.getCode())
                .set(Order::getCancelReason, trimmedReason)
                .set(Order::getCancelByAccountId, operatorAccountId)
                .set(Order::getCancelTime, now)
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, currentStatus));
        if (updated == 0) {
            throw new BusinessException("订单状态已变更，请刷新后重试");
        }

        taskMapper.update(null, new LambdaUpdateWrapper<Task>()
                .set(Task::getTaskStatus, TaskStatusEnum.TERMINATED.getCode())
                .eq(Task::getOrderId, order.getId())
                .in(Task::getTaskStatus, TaskStatusEnum.PENDING_ASSIGN.getCode(),
                        TaskStatusEnum.WAIT_EXECUTE.getCode()));

        notifyCanceled(order, trimmedReason, tasks);
    }

    /**
     * 向货主、接单物流公司与相关司机发送订单取消通知。
     *
     * @param order 订单实体
     * @param reason 取消原因
     * @param tasks 订单任务列表
     */
    private void notifyCanceled(Order order, String reason, List<Task> tasks) {
        Set<Long> receivers = new LinkedHashSet<>();
        receivers.add(order.getShipperAccountId());
        if (order.getLogisticsAccountId() != null) {
            receivers.add(order.getLogisticsAccountId());
        }
        List<Long> drivers = new ArrayList<>();
        for (Task task : tasks) {
            if (task.getDriverAccountId() != null) {
                drivers.add(task.getDriverAccountId());
            }
        }
        receivers.addAll(drivers);
        notificationService.sendAll(List.copyOf(receivers), NoticeTypeEnum.ORDER_CANCELED, "订单已取消",
                "订单" + order.getOrderNo() + "已取消，原因：" + reason, order.getId(), null);
    }
}
