package com.example.web.module.task.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.checkin.dto.DriverRecordQueryDTO;
import com.example.web.module.checkin.service.CheckinService;
import com.example.web.module.checkin.vo.DriverRecordVO;
import com.example.web.module.task.dto.TaskAbandonDTO;
import com.example.web.module.task.service.TaskDispatchService;
import com.example.web.module.task.vo.DriverTaskDetailVO;
import com.example.web.module.task.vo.DriverTaskListVO;
import com.example.web.module.task.vo.DriverTaskPoolVO;
import com.example.web.module.task.vo.TaskBriefVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机任务列表、任务池领取、知悉确认与弃单接口。
 */
@RestController
@RequestMapping("/api/driver")
public class ApiDriverTaskController {

    private final TaskDispatchService taskDispatchService;
    private final CheckinService checkinService;

    public ApiDriverTaskController(TaskDispatchService taskDispatchService, CheckinService checkinService) {
        this.taskDispatchService = taskDispatchService;
        this.checkinService = checkinService;
    }

    /**
     * 分页查询本人任务。
     *
     * @param tab 状态标签
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 司机任务分页数据
     */
    @GetMapping("/tasks")
    public Result<PageResult<DriverTaskListVO>> pageMyTasks(
            @RequestParam(required = false) String tab,
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(taskDispatchService.pageDriverTasks(tab, pageNum, pageSize));
    }

    /**
     * 分页查询本人全部现场记录，按打卡提交时间倒序。
     *
     * @param dto 查询参数
     * @return 现场记录分页数据
     */
    @GetMapping("/records")
    public Result<PageResult<DriverRecordVO>> pageRecords(DriverRecordQueryDTO dto) {
        return Result.success(checkinService.pageRecords(dto));
    }

    /**
     * 查询本人任务详情。
     *
     * @param id 任务 ID
     * @return 任务详情
     */
    @GetMapping("/tasks/{id}")
    public Result<DriverTaskDetailVO> getMyTask(@PathVariable Long id) {
        return Result.success(taskDispatchService.getDriverTaskDetail(id));
    }

    /**
     * 确认知悉指派。
     *
     * @param id 任务 ID
     * @return 任务概要
     */
    @PostMapping("/tasks/{id}/ack")
    public Result<TaskBriefVO> ack(@PathVariable Long id) {
        return Result.success(taskDispatchService.ack(id));
    }

    /**
     * 未出发时弃单。
     *
     * @param id 任务 ID
     * @param dto 弃单原因
     * @return 任务概要
     */
    @PostMapping("/tasks/{id}/abandon")
    public Result<TaskBriefVO> abandon(@PathVariable Long id, @Valid @RequestBody TaskAbandonDTO dto) {
        return Result.success(taskDispatchService.abandon(id, dto.getReason()));
    }

    /**
     * 查询本公司任务池公开概要。
     *
     * @param pageNum 页码
     * @param pageSize 每页条数
     * @return 任务池公开分页数据
     */
    @GetMapping("/task-pool")
    public Result<PageResult<DriverTaskPoolVO>> pageTaskPool(
            @RequestParam(required = false) Integer pageNum,
            @RequestParam(required = false) Integer pageSize) {
        return Result.success(taskDispatchService.pageTaskPool(pageNum, pageSize));
    }

    /**
     * 领取任务池任务。
     *
     * @param taskId 任务 ID
     * @return 任务概要
     */
    @PostMapping("/task-pool/{taskId}/claim")
    public Result<TaskBriefVO> claim(@PathVariable Long taskId) {
        return Result.success(taskDispatchService.claim(taskId));
    }
}
