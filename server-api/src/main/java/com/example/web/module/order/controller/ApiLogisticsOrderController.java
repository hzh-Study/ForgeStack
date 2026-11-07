package com.example.web.module.order.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.result.Result;
import com.example.web.module.order.dto.OrderCancelDTO;
import com.example.web.module.order.service.OrderLifecycleService;
import com.example.web.security.CurrentAccountService;
import com.example.web.security.RoleCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司订单操作接口。
 */
@RestController
@RequestMapping("/api/logistics/orders")
@SaCheckRole(RoleCode.LOGISTICS)
public class ApiLogisticsOrderController {

    private final OrderLifecycleService orderLifecycleService;
    private final CurrentAccountService currentAccountService;

    public ApiLogisticsOrderController(OrderLifecycleService orderLifecycleService,
                                       CurrentAccountService currentAccountService) {
        this.orderLifecycleService = orderLifecycleService;
        this.currentAccountService = currentAccountService;
    }

    /**
     * 物流公司取消本公司接单且全部任务未出发的订单。
     *
     * @param orderId 订单 ID
     * @param dto 取消原因
     * @return 空结果
     */
    @PostMapping("/{orderId}/cancel")
    public Result<Void> cancel(@PathVariable Long orderId, @Valid @RequestBody OrderCancelDTO dto) {
        orderLifecycleService.cancelByLogistics(orderId,
                currentAccountService.getBusinessAccount().getId(), dto.getReason());
        return Result.success();
    }
}
