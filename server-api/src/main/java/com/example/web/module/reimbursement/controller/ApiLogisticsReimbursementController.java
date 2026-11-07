package com.example.web.module.reimbursement.controller;

import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.reimbursement.dto.DriverQuotaDTO;
import com.example.web.module.reimbursement.dto.LogisticsReimbursementQueryDTO;
import com.example.web.module.reimbursement.dto.ReimbursementReviewDTO;
import com.example.web.module.reimbursement.service.ReimbursementService;
import com.example.web.module.reimbursement.vo.LogisticsReimbursementListVO;
import com.example.web.module.reimbursement.vo.ReimbursementDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 物流公司报销审核、特殊复核与司机月度额度覆盖接口。
 */
@RestController
@RequestMapping("/api/logistics")
public class ApiLogisticsReimbursementController {

    private final ReimbursementService reimbursementService;

    public ApiLogisticsReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 分页查询本公司司机报销单。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    @GetMapping("/reimbursements")
    public Result<PageResult<LogisticsReimbursementListVO>> pageReimbursements(
            LogisticsReimbursementQueryDTO dto) {
        return Result.success(reimbursementService.pageLogisticsReimbursements(dto));
    }

    /**
     * 查询报销单详情与审核历史。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    @GetMapping("/reimbursements/{id}")
    public Result<ReimbursementDetailVO> getReimbursement(@PathVariable Long id) {
        return Result.success(reimbursementService.getLogisticsReimbursementDetail(id));
    }

    /**
     * 审核报销单。
     *
     * @param id 报销单 ID
     * @param dto 审核参数
     * @return 空结果
     */
    @PostMapping("/reimbursements/{id}/review")
    public Result<Void> review(@PathVariable Long id, @Valid @RequestBody ReimbursementReviewDTO dto) {
        reimbursementService.review(id, dto);
        return Result.success();
    }

    /**
     * 按司机覆盖指定月份报销额度。
     *
     * @param dto 额度参数
     * @return 空结果
     */
    @PostMapping("/driver-quotas")
    public Result<Void> upsertDriverQuota(@Valid @RequestBody DriverQuotaDTO dto) {
        reimbursementService.upsertDriverQuota(dto);
        return Result.success();
    }
}
