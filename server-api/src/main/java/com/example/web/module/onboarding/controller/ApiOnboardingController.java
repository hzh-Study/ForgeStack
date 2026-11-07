package com.example.web.module.onboarding.controller;

import com.example.web.common.result.Result;
import com.example.web.module.onboarding.dto.OnboardingSubmitDTO;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.module.onboarding.vo.OnboardingDetailVO;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 货主与物流公司共用的入驻申请接口。
 */
@RestController
@RequestMapping("/api/onboarding")
public class ApiOnboardingController {

    private final OnboardingService onboardingService;

    public ApiOnboardingController(OnboardingService onboardingService) {
        this.onboardingService = onboardingService;
    }

    /**
     * 提交企业入驻申请。
     *
     * @param dto 入驻提交参数
     * @return 入驻申请详情
     */
    @PostMapping("/submit")
    public Result<OnboardingDetailVO> submit(@Valid @RequestBody OnboardingSubmitDTO dto) {
        return Result.success(onboardingService.submit(dto));
    }

    /**
     * 查询当前账号最近一次入驻申请。
     *
     * @return 入驻申请详情
     */
    @GetMapping
    public Result<OnboardingDetailVO> getCurrent() {
        return Result.success(onboardingService.getCurrentOnboarding());
    }
}
