package com.example.web.module.checkin.controller;

import com.example.web.common.result.Result;
import com.example.web.module.checkin.dto.CheckinRemarkDTO;
import com.example.web.module.checkin.dto.CheckinSubmitDTO;
import com.example.web.module.checkin.service.CheckinService;
import com.example.web.module.checkin.vo.CheckinRemarkVO;
import com.example.web.module.checkin.vo.CheckinResultVO;
import com.example.web.module.checkin.vo.TaskCheckinDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机出发打卡、到达打卡、追加说明与打卡记录查询接口。
 */
@RestController
@RequestMapping("/api/driver/tasks")
public class ApiDriverCheckinController {

    private final CheckinService checkinService;

    public ApiDriverCheckinController(CheckinService checkinService) {
        this.checkinService = checkinService;
    }

    /**
     * 提交出发打卡。
     *
     * @param id 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    @PostMapping("/{id}/checkin/depart")
    public Result<CheckinResultVO> depart(@PathVariable Long id, @Valid @RequestBody CheckinSubmitDTO dto) {
        return Result.success(checkinService.depart(id, dto));
    }

    /**
     * 提交到达打卡。
     *
     * @param id 任务 ID
     * @param dto 打卡参数
     * @return 打卡结果
     */
    @PostMapping("/{id}/checkin/arrive")
    public Result<CheckinResultVO> arrive(@PathVariable Long id, @Valid @RequestBody CheckinSubmitDTO dto) {
        return Result.success(checkinService.arrive(id, dto));
    }

    /**
     * 追加打卡说明。
     *
     * @param id 任务 ID
     * @param dto 追加说明参数
     * @return 追加说明
     */
    @PostMapping("/{id}/checkin/remark")
    public Result<CheckinRemarkVO> remark(@PathVariable Long id, @Valid @RequestBody CheckinRemarkDTO dto) {
        return Result.success(checkinService.addRemark(id, dto));
    }

    /**
     * 查询本人任务打卡记录。
     *
     * @param id 任务 ID
     * @return 打卡明细
     */
    @GetMapping("/{id}/checkins")
    public Result<TaskCheckinDetailVO> getCheckins(@PathVariable Long id) {
        return Result.success(checkinService.getCheckins(id));
    }
}
