package com.example.web.module.reimbursement.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.reimbursement.dto.ReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementSubmitDTO;
import com.example.web.module.reimbursement.service.ReimbursementService;
import com.example.web.module.reimbursement.vo.DriverQuotaVO;
import com.example.web.module.reimbursement.vo.DriverReimbursementListVO;
import com.example.web.module.reimbursement.vo.ReimbursementSubmitVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 司机报销提交、重新提交、我的报销与月度额度查询接口。
 */
@RestController
@RequestMapping("/api/driver/reimbursements")
public class ApiDriverReimbursementController {

    private final ReimbursementService reimbursementService;

    public ApiDriverReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 提交报销单。
     *
     * @param dto 报销提交参数
     * @return 提交结果
     */
    @PostMapping
    public Result<ReimbursementSubmitVO> submit(@Valid @RequestBody ReimbursementSubmitDTO dto) {
        return Result.success(reimbursementService.submit(dto));
    }

    /**
     * 分页查询本人报销单。
     *
     * @param dto 分页参数
     * @return 报销分页数据
     */
    @GetMapping
    public Result<PageResult<DriverReimbursementListVO>> pageReimbursements(ReimbursementQueryDTO dto) {
        return Result.success(reimbursementService.pageDriverReimbursements(dto));
    }

    /**
     * 查询本人指定月份额度与使用情况。
     *
     * @param yearMonth 年月，格式 yyyy-MM
     * @return 额度信息
     */
    @GetMapping("/quota")
    public Result<DriverQuotaVO> getQuota(@RequestParam(required = false) String yearMonth) {
        return Result.success(reimbursementService.getDriverQuota(yearMonth));
    }

    /**
     * 修改已驳回报销单并重新提交。
     *
     * @param id 报销单 ID
     * @param dto 报销提交参数
     * @return 新版本提交结果
     */
    @PostMapping("/{id}/resubmit")
    public Result<ReimbursementSubmitVO> resubmit(@PathVariable Long id,
                                                  @Valid @RequestBody ReimbursementSubmitDTO dto) {
        return Result.success(reimbursementService.resubmit(id, dto));
    }
}
