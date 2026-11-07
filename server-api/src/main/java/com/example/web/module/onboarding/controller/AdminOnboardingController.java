package com.example.web.module.onboarding.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.example.web.common.page.PageResult;
import com.example.web.common.result.Result;
import com.example.web.module.onboarding.dto.OnboardingQueryDTO;
import com.example.web.module.onboarding.dto.OnboardingRejectDTO;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.module.onboarding.vo.OnboardingDetailVO;
import com.example.web.module.onboarding.vo.OnboardingListVO;
import com.example.web.security.RoleCode;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 后台入驻申请审核接口。
 */
@RestController
@RequestMapping("/admin/onboardings")
public class AdminOnboardingController {

    private final OnboardingService onboardingService;

    public AdminOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * 分页查询入驻申请。
     *
     * @param dto 查询参数
     * @return 入驻申请分页数据
     */
    @GetMapping
    @SaCheckRole(RoleCode.ADMIN)
    public Result<PageResult<OnboardingListVO>> pageOnboardings(OnboardingQueryDTO dto) {
        return Result.success(onboardingService.pageOnboardings(dto));
    }

    /**
     * 查询入驻申请详情。
     *
     * @param id 入驻申请 ID
     * @return 入驻申请详情
     */
    @GetMapping("/{id}")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<OnboardingDetailVO> getOnboardingDetail(@PathVariable Long id) {
        return Result.success(onboardingService.getOnboardingDetail(id));
    }

    /**
     * 审核通过入驻申请。
     *
     * @param id 入驻申请 ID
     * @return 空结果
     */
    @PostMapping("/{id}/approve")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Void> approve(@PathVariable Long id) {
        onboardingService.approve(id);
        return Result.success();
    }

    /**
     * 驳回入驻申请。
     *
     * @param id 入驻申请 ID
     * @param dto 驳回参数
     * @return 空结果
     */
    @PostMapping("/{id}/reject")
    @SaCheckRole(RoleCode.ADMIN)
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody OnboardingRejectDTO dto) {
        onboardingService.reject(id, dto.getReason());
        return Result.success();
    }
}
