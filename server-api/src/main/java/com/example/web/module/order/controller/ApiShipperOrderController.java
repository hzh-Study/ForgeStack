package com.example.web.module.order.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.order.dto.OrderCancelDTO;
import com.example.web.module.order.dto.OrderSaveDTO;
import com.example.web.module.order.dto.ShipperOrderQueryDTO;
import com.example.web.module.order.service.ShipperOrderService;
import com.example.web.module.order.vo.OrderSaveVO;
import com.example.web.module.order.vo.ShipperOrderDetailVO;
import com.example.web.module.order.vo.ShipperOrderListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 货主发货与我的订单接口。
 */
@RestController
@RequestMapping("/api/shipper/orders")
public class ApiShipperOrderController {

    private final ShipperOrderService shipperOrderService;

    public ApiShipperOrderController(ShipperOrderService shipperOrderService) {
        this.shipperOrderService = shipperOrderService;
    }

    /**
     * 保存或编辑发货草稿。
     *
     * @param dto 发货单参数
     * @return 订单 ID 与状态
     */
    @PostMapping("/save")
    public Result<OrderSaveVO> save(@Valid @RequestBody OrderSaveDTO dto) {
        return Result.success(shipperOrderService.save(dto));
    }

    /**
     * 发布本人草稿进入订单池。
     *
     * @param id 订单 ID
     * @return 空结果
     */
    @PostMapping("/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        shipperOrderService.publish(id);
        return Result.success();
    }

    /**
     * 撤回本人待接单订单。
     *
     * @param id 订单 ID
     * @return 空结果
     */
    @PostMapping("/{id}/withdraw")
    public Result<Void> withdraw(@PathVariable Long id) {
        shipperOrderService.withdraw(id);
        return Result.success();
    }

    /**
     * 货主取消已接单且全部任务未出发的订单。
     *
     * @param id 订单 ID
     * @param dto 取消原因
     * @return 空结果
     */
    @PostMapping("/{id}/cancel")
    public Result<Void> cancel(@PathVariable Long id, @Valid @RequestBody OrderCancelDTO dto) {
        shipperOrderService.cancel(id, dto.getReason());
        return Result.success();
    }

    /**
     * 将本人已下架订单重新发布为新订单。
     *
     * @param id 原订单 ID
     * @return 新订单 ID 与状态
     */
    @PostMapping("/{id}/republish")
    public Result<OrderSaveVO> republish(@PathVariable Long id) {
        return Result.success(shipperOrderService.republish(id));
    }

    /**
     * 分页查询本人订单。
     *
     * @param dto 查询参数
     * @return 订单概要分页数据
     */
    @GetMapping
    public Result<PageResult<ShipperOrderListVO>> pageMyOrders(ShipperOrderQueryDTO dto) {
        return Result.success(shipperOrderService.pageMyOrders(dto));
    }

    /**
     * 查询本人订单完整详情。
     *
     * @param id 订单 ID
     * @return 订单详情
     */
    @GetMapping("/{id}")
    public Result<ShipperOrderDetailVO> getMyOrder(@PathVariable Long id) {
        return Result.success(shipperOrderService.getMyOrder(id));
    }
}
