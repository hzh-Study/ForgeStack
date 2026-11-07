package com.example.web.module.account.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.account.dto.AccountQueryDTO;
import com.example.web.module.account.dto.AdminAccountCreateDTO;
import com.example.web.module.account.service.AccountGovernanceService;
import com.example.web.module.account.vo.AccountListVO;
import com.example.web.security.RoleCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台四类账号治理接口。
 */
@RestController
@RequestMapping("/admin/accounts")
public class AdminAccountController {

    private final AccountGovernanceService accountGovernanceService;

    public AdminAccountController(AccountGovernanceService accountGovernanceService) {
        this.accountGovernanceService = accountGovernanceService;
    }

    /**
     * 分页查询四类账号。
     *
     * @param dto 查询参数
     * @return 账号分页数据
     */
    @GetMapping
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<AccountListVO>> pageAccounts(AccountQueryDTO dto) {
        return Result.success(accountGovernanceService.pageAccounts(dto));
    }

    /**
     * 创建管理员账号。
     *
     * @param dto 创建参数
     * @return 新建账号 ID
     */
    @PostMapping("/admin")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Long> createAdminAccount(@Valid @RequestBody AdminAccountCreateDTO dto) {
        return Result.success(accountGovernanceService.createAdminAccount(dto));
    }

    /**
     * 停用指定账号。
     *
     * @param accountId 账号 ID
     * @return 空结果
     */
    @PostMapping("/{accountId}/disable")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Void> disableAccount(@PathVariable Long accountId) {
        accountGovernanceService.disableAccount(accountId);
        return Result.success();
    }

    /**
     * 启用指定账号。
     *
     * @param accountId 账号 ID
     * @return 空结果
     */
    @PostMapping("/{accountId}/enable")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Void> enableAccount(@PathVariable Long accountId) {
        accountGovernanceService.enableAccount(accountId);
        return Result.success();
    }
}
