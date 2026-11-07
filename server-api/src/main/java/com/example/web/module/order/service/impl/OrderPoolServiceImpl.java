package com.example.web.module.order.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.page.PageResult;
import com.example.web.common.time.ClockService;
import com.example.web.common.util.BusinessNoGenerator;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.message.enums.NoticeTypeEnum;
import com.example.web.module.message.service.NotificationService;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.module.order.dto.OrderPoolQueryDTO;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.entity.OrderAddress;
import com.example.web.module.order.entity.OrderGoods;
import com.example.web.module.order.enums.AddressTypeEnum;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderAddressMapper;
import com.example.web.module.order.mapper.OrderGoodsMapper;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.order.service.OrderPoolService;
import com.example.web.module.order.vo.OrderPoolDetailVO;
import com.example.web.module.order.vo.OrderPoolGoodsVO;
import com.example.web.module.order.vo.OrderPoolListVO;
import com.example.web.security.CurrentAccountService;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 订单池业务实现，公开投影只展示接单判断所需信息并严格隐藏货主隐私。
 */
@Service
public class OrderPoolServiceImpl implements OrderPoolService {

    private final CurrentAccountService currentAccountService;
    private final OnboardingService onboardingService;
    private final OrderMapper orderMapper;
    private final OrderGoodsMapper orderGoodsMapper;
    private final OrderAddressMapper orderAddressMapper;
    private final ClockService clockService;
    private final BusinessNoGenerator businessNoGenerator;
    private final NotificationService notificationService;

    /**
     * 注入订单池浏览与接单所需依赖。
     *
     * @param currentAccountService 当前账号服务
     * @param onboardingService 入驻资格校验服务
     * @param orderMapper 订单数据访问
     * @param orderGoodsMapper 货物明细数据访问
     * @param orderAddressMapper 地址数据访问
     * @param clockService 业务时间服务
     * @param businessNoGenerator 业务单号生成器
     * @param notificationService 系统通知服务
     */
    public OrderPoolServiceImpl(CurrentAccountService currentAccountService,
                                OnboardingService onboardingService, OrderMapper orderMapper,
                                OrderGoodsMapper orderGoodsMapper, OrderAddressMapper orderAddressMapper,
                                ClockService clockService, BusinessNoGenerator businessNoGenerator,
                                NotificationService notificationService) {
        this.currentAccountService = currentAccountService;
        this.onboardingService = onboardingService;
        this.orderMapper = orderMapper;
        this.orderGoodsMapper = orderGoodsMapper;
        this.orderAddressMapper = orderAddressMapper;
        this.clockService = clockService;
        this.businessNoGenerator = businessNoGenerator;
        this.notificationService = notificationService;
    }

    /**
     * 分页查询订单池，先按地址与货物类型求订单 ID 交集，再结合状态、运费与排序查询。
     *
     * @param dto 查询参数
     * @return 订单池公开分页数据
     */
    @Override
    public PageResult<OrderPoolListVO> pageOrderPool(OrderPoolQueryDTO dto) {
        Account account = requireApprovedLogistics();
        LocalDateTime now = clockService.now();
        Set<Long> filteredIds = searchCandidateOrderIds(dto);
        if (filteredIds != null && filteredIds.isEmpty()) {
            return new PageResult<>(Collections.emptyList(), 0L, dto.getPageNum(), dto.getPageSize());
        }

        LambdaQueryWrapper<Order> wrapper = new LambdaQueryWrapper<Order>()
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode())
                .gt(Order::getExpireTime, now);
        if (filteredIds != null) {
            wrapper.in(Order::getId, filteredIds);
        }
        if (dto.getMinFreight() != null) {
            wrapper.ge(Order::getFreightAmount, dto.getMinFreight());
        }
        if (dto.getMaxFreight() != null) {
            wrapper.le(Order::getFreightAmount, dto.getMaxFreight());
        }
        applySort(wrapper, dto.getSortBy(), dto.getSortOrder());
        Page<Order> page = orderMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()), wrapper);

        Map<Long, List<OrderGoods>> goodsMap = loadGoodsMap(page.getRecords());
        Map<Long, List<OrderAddress>> addressMap = loadAddressMap(page.getRecords());
        List<OrderPoolListVO> records = page.getRecords().stream()
                .map(order -> toListVO(order, now, goodsMap.get(order.getId()), addressMap.get(order.getId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 查询订单池公开详情，非待接单或已过期订单返回明确错误。
     *
     * @param orderId 订单 ID
     * @return 公开详情
     */
    @Override
    public OrderPoolDetailVO getOrderPoolDetail(Long orderId) {
        requireApprovedLogistics();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        ensurePendingAndUnexpired(order);
        return toDetailVO(order, clockService.now(), orderQueryGoods(order.getId()),
                orderQueryAddresses(order.getId()));
    }

    /**
     * 接单：条件更新待接单订单，写入承运公司、接单时间、状态与运单号，并通知货主。
     *
     * @param orderId 订单 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void accept(Long orderId) {
        Account account = requireApprovedLogistics();
        Order order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException("订单不存在");
        }
        ensureAcceptable(order);
        LocalDateTime now = clockService.now();
        String waybillNo = businessNoGenerator.nextWaybillNo();
        int updated = orderMapper.update(null, new LambdaUpdateWrapper<Order>()
                .set(Order::getLogisticsAccountId, account.getId())
                .set(Order::getAcceptTime, now)
                .set(Order::getOrderStatus, OrderStatusEnum.ACCEPTED.getCode())
                .set(Order::getWaybillNo, waybillNo)
                .eq(Order::getId, order.getId())
                .eq(Order::getOrderStatus, OrderStatusEnum.PENDING_ACCEPT.getCode())
                .gt(Order::getExpireTime, now));
        if (updated == 0) {
            throw new BusinessException("订单已被接走");
        }
        notificationService.send(order.getShipperAccountId(), NoticeTypeEnum.ORDER_ACCEPTED, "订单已被接单",
                "订单" + order.getOrderNo() + "已被物流公司接单，运单号" + waybillNo, order.getId(), null);
    }

    /**
     * 校验当前账号为已入驻通过的物流公司。
     *
     * @return 当前物流公司账号
     */
    private Account requireApprovedLogistics() {
        Account account = currentAccountService.requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        onboardingService.assertApproved(account.getId(), AccountRoleEnum.LOGISTICS);
        return account;
    }

    /**
     * 校验订单必须处于待接单且未过期状态。
     *
     * @param order 订单实体
     */
    private void ensurePendingAndUnexpired(Order order) {
        if (!OrderStatusEnum.PENDING_ACCEPT.getCode().equals(order.getOrderStatus())) {
            throw new BusinessException("订单当前状态不可接单");
        }
        if (order.getExpireTime() == null || !clockService.now().isBefore(order.getExpireTime())) {
            throw new BusinessException("订单已过期，无法接单");
        }
    }

    /**
     * 校验订单可接单：已归属其他公司的订单统一提示已被接走，其余非待接单状态提示不可接单。
     *
     * @param order 订单实体
     */
    private void ensureAcceptable(Order order) {
        String status = order.getOrderStatus();
        if (!OrderStatusEnum.PENDING_ACCEPT.getCode().equals(status)) {
            if (OrderStatusEnum.ACCEPTED.getCode().equals(status)
                    || OrderStatusEnum.DISPATCHING.getCode().equals(status)
                    || OrderStatusEnum.IN_PROGRESS.getCode().equals(status)
                    || OrderStatusEnum.COMPLETED.getCode().equals(status)) {
                throw new BusinessException("订单已被接走");
            }
            throw new BusinessException("订单当前状态不可接单");
        }
        if (order.getExpireTime() == null || !clockService.now().isBefore(order.getExpireTime())) {
            throw new BusinessException("订单已过期，无法接单");
        }
    }

    /**
     * 按地址与货物类型筛选候选订单 ID，未使用任何筛选时返回 null。
     *
     * @param dto 查询参数
     * @return 候选订单 ID 交集，未筛选时返回 null
     */
    private Set<Long> searchCandidateOrderIds(OrderPoolQueryDTO dto) {
        Set<Long> result = null;
        if (isNotBlank(dto.getPickupProvince()) || isNotBlank(dto.getPickupCity())) {
            result = intersect(result, addressOrderIds(AddressTypeEnum.PICKUP, dto.getPickupProvince(),
                    dto.getPickupCity()));
        }
        if (isNotBlank(dto.getDeliveryProvince()) || isNotBlank(dto.getDeliveryCity())) {
            result = intersect(result, addressOrderIds(AddressTypeEnum.DELIVERY, dto.getDeliveryProvince(),
                    dto.getDeliveryCity()));
        }
        if (isNotBlank(dto.getGoodsType())) {
            result = intersect(result, goodsTypeOrderIds(dto.getGoodsType()));
        }
        return result;
    }

    /**
     * 按地址类型与省市区查询订单 ID 集合。
     *
     * @param type 地址类型
     * @param province 省
     * @param city 市
     * @return 订单 ID 集合
     */
    private Set<Long> addressOrderIds(AddressTypeEnum type, String province, String city) {
        LambdaQueryWrapper<OrderAddress> wrapper = new LambdaQueryWrapper<OrderAddress>()
                .eq(OrderAddress::getAddressType, type.getCode());
        if (isNotBlank(province)) {
            wrapper.eq(OrderAddress::getProvince, province.trim());
        }
        if (isNotBlank(city)) {
            wrapper.eq(OrderAddress::getCity, city.trim());
        }
        return orderAddressMapper.selectList(wrapper).stream().map(OrderAddress::getOrderId)
                .collect(Collectors.toCollection(HashSet::new));
    }

    /**
     * 按货物类型查询订单 ID 集合。
     *
     * @param goodsType 货物类型编码
     * @return 订单 ID 集合
     */
    private Set<Long> goodsTypeOrderIds(String goodsType) {
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .eq(OrderGoods::getGoodsType, goodsType.trim()))
                .stream().map(OrderGoods::getOrderId).collect(Collectors.toCollection(LinkedHashSet::new));
    }

    /**
     * 求订单 ID 交集，首次筛选直接返回该集合。
     *
     * @param current 当前交集
     * @param next 下一次筛选集合
     * @return 交集结果
     */
    private Set<Long> intersect(Set<Long> current, Set<Long> next) {
        if (current == null) {
            return next;
        }
        current.retainAll(next);
        return current;
    }

    /**
     * 应用排序字段与方向，非法值返回明确提示。
     *
     * @param wrapper 查询条件
     * @param sortBy 排序字段
     * @param sortOrder 排序方向
     */
    private void applySort(LambdaQueryWrapper<Order> wrapper, String sortBy, String sortOrder) {
        String field = sortBy == null || sortBy.isBlank() ? "publishTime" : sortBy.trim();
        boolean asc = "asc".equalsIgnoreCase(sortOrder);
        if (sortOrder != null && !sortOrder.isBlank() && !asc && !"desc".equalsIgnoreCase(sortOrder)) {
            throw new BusinessException("排序方向不支持");
        }
        switch (field) {
            case "publishTime" -> wrapper.orderBy(true, asc, Order::getPublishTime);
            case "expireTime" -> wrapper.orderBy(true, asc, Order::getExpireTime);
            case "freightAmount" -> wrapper.orderBy(true, asc, Order::getFreightAmount);
            default -> throw new BusinessException("排序字段不支持");
        }
        if (asc) {
            wrapper.orderByAsc(Order::getId);
        } else {
            wrapper.orderByDesc(Order::getId);
        }
    }

    /**
     * 判断字符串是否非空。
     *
     * @param value 字符串
     * @return 是否非空
     */
    private boolean isNotBlank(String value) {
        return value != null && !value.isBlank();
    }

    /**
     * 批量加载分页订单的货物明细。
     *
     * @param orders 订单列表
     * @return 订单 ID 到货物明细映射
     */
    private Map<Long, List<OrderGoods>> loadGoodsMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                        .in(OrderGoods::getOrderId, orderIds)
                        .orderByAsc(OrderGoods::getId))
                .stream().collect(Collectors.groupingBy(OrderGoods::getOrderId));
    }

    /**
     * 批量加载分页订单的地址。
     *
     * @param orders 订单列表
     * @return 订单 ID 到地址映射
     */
    private Map<Long, List<OrderAddress>> loadAddressMap(List<Order> orders) {
        List<Long> orderIds = orders.stream().map(Order::getId).toList();
        if (orderIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                        .in(OrderAddress::getOrderId, orderIds)
                        .orderByAsc(OrderAddress::getId))
                .stream().collect(Collectors.groupingBy(OrderAddress::getOrderId));
    }

    /**
     * 查询单订单货物明细。
     *
     * @param orderId 订单 ID
     * @return 货物明细
     */
    private List<OrderGoods> orderQueryGoods(Long orderId) {
        return orderGoodsMapper.selectList(new LambdaQueryWrapper<OrderGoods>()
                .eq(OrderGoods::getOrderId, orderId).orderByAsc(OrderGoods::getId));
    }

    /**
     * 查询单订单地址。
     *
     * @param orderId 订单 ID
     * @return 地址列表
     */
    private List<OrderAddress> orderQueryAddresses(Long orderId) {
        return orderAddressMapper.selectList(new LambdaQueryWrapper<OrderAddress>()
                .eq(OrderAddress::getOrderId, orderId).orderByAsc(OrderAddress::getId));
    }

    /**
     * 构造订单池列表公开投影。
     *
     * @param order 订单实体
     * @param now 当前业务时间
     * @param goods 货物明细
     * @param addresses 地址列表
     * @return 列表公开投影
     */
    private OrderPoolListVO toListVO(Order order, LocalDateTime now, List<OrderGoods> goods,
                                     List<OrderAddress> addresses) {
        OrderPoolListVO vo = new OrderPoolListVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        fillListAddress(vo, addresses);
        vo.setGoods(toPoolGoods(goods));
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPublishTime(order.getPublishTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setValidHours(order.getValidHours());
        fillRemaining(vo::setRemainingSeconds, vo::setRemainingMinutes, now, order.getExpireTime());
        return vo;
    }

    /**
     * 构造订单池详情公开投影。
     *
     * @param order 订单实体
     * @param now 当前业务时间
     * @param goods 货物明细
     * @param addresses 地址列表
     * @return 详情公开投影
     */
    private OrderPoolDetailVO toDetailVO(Order order, LocalDateTime now, List<OrderGoods> goods,
                                         List<OrderAddress> addresses) {
        OrderPoolDetailVO vo = new OrderPoolDetailVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        fillDetailAddress(vo, addresses);
        vo.setGoods(toPoolGoods(goods));
        vo.setPickupTime(order.getPickupTime());
        vo.setDeliveryTime(order.getDeliveryTime());
        vo.setFreightAmount(order.getFreightAmount());
        vo.setPublishTime(order.getPublishTime());
        vo.setExpireTime(order.getExpireTime());
        vo.setValidHours(order.getValidHours());
        fillRemaining(vo::setRemainingSeconds, vo::setRemainingMinutes, now, order.getExpireTime());
        return vo;
    }

    /**
     * 将货物实体转换为公开货物概要。
     *
     * @param goods 货物明细
     * @return 公开货物概要列表
     */
    private List<OrderPoolGoodsVO> toPoolGoods(List<OrderGoods> goods) {
        if (goods == null) {
            return Collections.emptyList();
        }
        return goods.stream().map(item -> {
            OrderPoolGoodsVO vo = new OrderPoolGoodsVO();
            vo.setGoodsName(item.getGoodsName());
            vo.setGoodsType(item.getGoodsType());
            vo.setQuantity(item.getQuantity());
            vo.setUnit(item.getUnit());
            return vo;
        }).toList();
    }

    /**
     * 计算列表公开投影中的剩余时间。
     *
     * @param secondsSetter 剩余秒数写入方法
     * @param minutesSetter 剩余分钟数写入方法
     * @param now 当前业务时间
     * @param expireTime 到期时间
     */
    private void fillRemaining(java.util.function.Consumer<Long> secondsSetter,
                               java.util.function.Consumer<Long> minutesSetter,
                               LocalDateTime now, LocalDateTime expireTime) {
        long seconds = expireTime == null ? 0 : Duration.between(now, expireTime).getSeconds();
        secondsSetter.accept(Math.max(seconds, 0));
        minutesSetter.accept(Math.max(seconds / 60, 0));
    }

    /**
     * 填充列表中的装卸货省市区概要。
     *
     * @param vo 列表投影
     * @param addresses 地址列表
     */
    private void fillListAddress(OrderPoolListVO vo, List<OrderAddress> addresses) {
        if (addresses == null) {
            return;
        }
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

    /**
     * 填充详情中的装卸货省市区概要。
     *
     * @param vo 详情投影
     * @param addresses 地址列表
     */
    private void fillDetailAddress(OrderPoolDetailVO vo, List<OrderAddress> addresses) {
        if (addresses == null) {
            return;
        }
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
}
