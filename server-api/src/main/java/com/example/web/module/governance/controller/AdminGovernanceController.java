package com.example.web.module.governance.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.checkin.vo.CheckinVersionVO;
import com.example.web.module.governance.dto.AdminCheckinQueryDTO;
import com.example.web.module.governance.dto.AdminOrderQueryDTO;
import com.example.web.module.governance.dto.AdminTaskQueryDTO;
import com.example.web.module.governance.dto.DictItemSaveDTO;
import com.example.web.module.governance.dto.OperationLogQueryDTO;
import com.example.web.module.governance.dto.PlatformParamSaveDTO;
import com.example.web.module.governance.service.AdminGovernanceService;
import com.example.web.module.governance.vo.AdminOrderDetailVO;
import com.example.web.module.governance.vo.AdminOrderListVO;
import com.example.web.module.governance.vo.AdminTaskListVO;
import com.example.web.module.governance.vo.DictItemVO;
import com.example.web.module.governance.vo.OperationLogVO;
import com.example.web.module.governance.vo.PlatformParamVO;
import com.example.web.security.RoleCode;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员全平台只读监管、基础资料维护与平台参数维护接口。
 */
@RestController
@RequestMapping("/admin")
public class AdminGovernanceController {

    private final AdminGovernanceService adminGovernanceService;

    public AdminGovernanceController(AdminGovernanceService adminGovernanceService) {
        this.adminGovernanceService = adminGovernanceService;
    }

    /**
     * 分页查询全平台订单。
     *
     * @param dto 查询参数
     * @return 订单分页数据
     */
    @GetMapping("/orders")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<AdminOrderListVO>> pageOrders(AdminOrderQueryDTO dto) {
        return Result.success(adminGovernanceService.pageOrders(dto));
    }

    /**
     * 查询订单全链路详情。
     *
     * @param id 订单 ID
     * @return 全链路详情
     */
    @GetMapping("/orders/{id}")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<AdminOrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(adminGovernanceService.getOrderDetail(id));
    }

    /**
     * 分页查询全平台任务。
     *
     * @param dto 查询参数
     * @return 任务分页数据
     */
    @GetMapping("/tasks")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<AdminTaskListVO>> pageTasks(AdminTaskQueryDTO dto) {
        return Result.success(adminGovernanceService.pageTasks(dto));
    }

    /**
     * 分页查询全平台打卡版本。
     *
     * @param dto 查询参数
     * @return 打卡分页数据
     */
    @GetMapping("/checkins")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<CheckinVersionVO>> pageCheckins(AdminCheckinQueryDTO dto) {
        return Result.success(adminGovernanceService.pageCheckins(dto));
    }

    /**
     * 分页查询操作留痕。
     *
     * @param dto 查询参数
     * @return 操作留痕分页数据
     */
    @GetMapping("/operation-logs")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<OperationLogVO>> pageOperationLogs(OperationLogQueryDTO dto) {
        return Result.success(adminGovernanceService.pageOperationLogs(dto));
    }

    /**
     * 查询字典项。
     *
     * @param dictType 字典类型
     * @param status 状态
     * @return 字典项列表
     */
    @GetMapping("/dict-items")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<List<DictItemVO>> listDictItems(@RequestParam(required = false) String dictType,
                                                 @RequestParam(required = false) String status) {
        return Result.success(adminGovernanceService.listDictItems(dictType, status));
    }

    /**
     * 新增或修改字典项。
     *
     * @param dto 字典项参数
     * @return 字典项 ID
     */
    @PostMapping("/dict-items")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Long> saveDictItem(@Valid @RequestBody DictItemSaveDTO dto) {
        return Result.success(adminGovernanceService.saveDictItem(dto));
    }

    /**
     * 查询全部平台参数。
     *
     * @return 平台参数列表
     */
    @GetMapping("/platform-params")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<List<PlatformParamVO>> listPlatformParams() {
        return Result.success(adminGovernanceService.listPlatformParams());
    }

    /**
     * 新增或修改平台参数。
     *
     * @param dto 平台参数
     * @return 参数 ID
     */
    @PostMapping("/platform-params")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Long> savePlatformParam(@Valid @RequestBody PlatformParamSaveDTO dto) {
        return Result.success(adminGovernanceService.savePlatformParam(dto));
    }
}
