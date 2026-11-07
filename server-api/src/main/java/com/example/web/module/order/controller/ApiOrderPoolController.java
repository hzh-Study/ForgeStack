package com.example.web.module.order.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.order.dto.OrderPoolQueryDTO;
import com.example.web.module.order.service.OrderPoolService;
import com.example.web.module.order.vo.OrderPoolDetailVO;
import com.example.web.module.order.vo.OrderPoolListVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司订单池浏览与接单接口。
 */
@RestController
@RequestMapping("/api/logistics/order-pool")
public class ApiOrderPoolController {

    private final OrderPoolService orderPoolService;

    public ApiOrderPoolController(OrderPoolService orderPoolService) {
        this.orderPoolService = orderPoolService;
    }

    /**
     * 分页查询订单池公开信息。
     *
     * @param dto 查询参数
     * @return 订单池公开分页数据
     */
    @GetMapping
    public Result<PageResult<OrderPoolListVO>> pageOrderPool(OrderPoolQueryDTO dto) {
        return Result.success(orderPoolService.pageOrderPool(dto));
    }

    /**
     * 查询订单池公开详情。
     *
     * @param id 订单 ID
     * @return 公开详情
     */
    @GetMapping("/{id}")
    public Result<OrderPoolDetailVO> getOrderPoolDetail(@PathVariable Long id) {
        return Result.success(orderPoolService.getOrderPoolDetail(id));
    }

    /**
     * 物流公司接单。
     *
     * @param id 订单 ID
     * @return 空结果
     */
    @PostMapping("/{id}/accept")
    public Result<Void> accept(@PathVariable Long id) {
        orderPoolService.accept(id);
        return Result.success();
    }
}
