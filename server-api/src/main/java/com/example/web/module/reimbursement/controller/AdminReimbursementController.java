package com.example.web.module.reimbursement.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.reimbursement.dto.AdminReimbursementQueryDTO;
import com.example.web.module.reimbursement.service.ReimbursementService;
import com.example.web.module.reimbursement.vo.AdminReimbursementListVO;
import com.example.web.module.reimbursement.vo.ReimbursementDetailVO;
import com.example.web.security.RoleCode;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理员全平台报销只读监管接口。
 */
@RestController
@RequestMapping("/admin/reimbursements")
public class AdminReimbursementController {

    private final ReimbursementService reimbursementService;

    public AdminReimbursementController(ReimbursementService reimbursementService) {
        this.reimbursementService = reimbursementService;
    }

    /**
     * 分页查询全平台报销单。
     *
     * @param dto 查询参数
     * @return 报销分页数据
     */
    @GetMapping
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<AdminReimbursementListVO>> pageReimbursements(AdminReimbursementQueryDTO dto) {
        return Result.success(reimbursementService.pageAdminReimbursements(dto));
    }

    /**
     * 查询报销单与审核历史。
     *
     * @param id 报销单 ID
     * @return 报销详情
     */
    @GetMapping("/{id}")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<ReimbursementDetailVO> getReimbursement(@PathVariable Long id) {
        return Result.success(reimbursementService.getAdminReimbursementDetail(id));
    }
}
