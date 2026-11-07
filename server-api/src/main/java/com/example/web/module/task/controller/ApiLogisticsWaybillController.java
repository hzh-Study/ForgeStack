package com.example.web.module.task.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.service.WaybillService;
import com.example.web.module.task.vo.TaskBriefVO;
import com.example.web.module.task.vo.WaybillDetailVO;
import com.example.web.module.task.vo.WaybillListVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司运单查询与拆单接口。
 */
@RestController
@RequestMapping("/api/logistics/waybills")
public class ApiLogisticsWaybillController {

    private final WaybillService waybillService;

    public ApiLogisticsWaybillController(WaybillService waybillService) {
        this.waybillService = waybillService;
    }

    /**
     * 分页查询本公司运单。
     *
     * @param orderStatus 订单状态
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 运单列表分页数据
     */
    @GetMapping
    public Result<PageResult<WaybillListVO>> pageWaybills(
            @RequestParam(required = false) String orderStatus,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(waybillService.pageWaybills(orderStatus, pageNum, pageSize));
    }

    /**
     * 查询运单详情。
     *
     * @param orderId 订单 ID
     * @return 运单详情
     */
    @GetMapping("/{orderId}")
    public Result<WaybillDetailVO> getWaybillDetail(@PathVariable Long orderId) {
        return Result.success(waybillService.getWaybillDetail(orderId));
    }

    /**
     * 按货物明细整行创建运输任务。
     *
     * @param orderId 订单 ID
     * @param dto 货物行参数
     * @return 新建任务概要
     */
    @PostMapping("/{orderId}/tasks")
    public Result<TaskBriefVO> createTask(@PathVariable Long orderId, @Valid @RequestBody GoodsIdsDTO dto) {
        return Result.success(waybillService.createTask(orderId, dto));
    }
}
