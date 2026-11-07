package com.example.web.module.task.controller;

import com.example.web.common.result.Result;
import com.example.web.module.task.dto.GoodsIdsDTO;
import com.example.web.module.task.dto.TaskDispatchDTO;
import com.example.web.module.task.dto.TaskReclaimDTO;
import com.example.web.module.task.service.TaskDispatchService;
import com.example.web.module.task.vo.TaskBriefVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司任务指派、开放自抢、回收与货物调整接口。
 */
@RestController
@RequestMapping("/api/logistics/tasks")
public class ApiLogisticsTaskController {

    private final TaskDispatchService taskDispatchService;

    public ApiLogisticsTaskController(TaskDispatchService taskDispatchService) {
        this.taskDispatchService = taskDispatchService;
    }

    /**
     * 指派任务给本公司司机及绑定车辆。
     *
     * @param taskId 任务 ID
     * @param dto 司机与车辆参数
     * @return 任务概要
     */
    @PostMapping("/{taskId}/dispatch")
    public Result<TaskBriefVO> dispatch(@PathVariable Long taskId, @Valid @RequestBody TaskDispatchDTO dto) {
        return Result.success(taskDispatchService.dispatch(taskId, dto));
    }

    /**
     * 开放任务到公司任务池。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    @PostMapping("/{taskId}/open-claim")
    public Result<TaskBriefVO> openClaim(@PathVariable Long taskId) {
        return Result.success(taskDispatchService.openClaim(taskId));
    }

    /**
     * 强制回收未出发任务。
     *
     * @param taskId 任务 ID
     * @param dto 回收原因
     * @return 任务概要
     */
    @PostMapping("/{taskId}/reclaim")
    public Result<TaskBriefVO> reclaim(@PathVariable Long taskId, @Valid @RequestBody TaskReclaimDTO dto) {
        return Result.success(taskDispatchService.reclaim(taskId, dto.getReason()));
    }

    /**
     * 调整未出发任务承载的货物。
     *
     * @param taskId 任务 ID
     * @param dto 货物行参数
     * @return 任务概要
     */
    @PostMapping("/{taskId}/goods")
    public Result<TaskBriefVO> adjustGoods(@PathVariable Long taskId, @Valid @RequestBody GoodsIdsDTO dto) {
        return Result.success(taskDispatchService.adjustGoods(taskId, dto));
    }
}
