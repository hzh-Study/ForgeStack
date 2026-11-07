package com.example.web.module.onboarding.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.onboarding.dto.OnboardingQueryDTO;
import com.example.web.module.onboarding.dto.OnboardingSubmitDTO;
import com.example.web.module.onboarding.vo.OnboardingDetailVO;
import com.example.web.module.onboarding.vo.OnboardingListVO;

/**
 * 企业入驻申请与审核业务接口。
 */
public interface OnboardingService {

    /**
     * 提交当前账号的企业入驻申请，要求个人实名已通过。
     *
     * @param dto 入驻提交参数
     * @return 入驻申请详情
     */
    OnboardingDetailVO submit(OnboardingSubmitDTO dto);

    /**
     * 查询当前账号最近一次入驻申请。
     *
     * @return 入驻申请详情，未提交时返回 null
     */
    OnboardingDetailVO getCurrentOnboarding();

    /**
     * 管理员分页查询入驻申请，列表证件号脱敏。
     *
     * @param dto 查询参数
     * @return 入驻申请分页数据
     */
    PageResult<OnboardingListVO> pageOnboardings(OnboardingQueryDTO dto);

    /**
     * 管理员查询入驻申请详情，可见完整证件号。
     *
     * @param id 入驻申请 ID
     * @return 入驻申请详情
     */
    OnboardingDetailVO getOnboardingDetail(Long id);

    /**
     * 管理员审核通过入驻申请并通知申请账号。
     *
     * @param id 入驻申请 ID
     */
    void approve(Long id);

    /**
     * 管理员驳回入驻申请并记录驳回原因。
     *
     * @param id 入驻申请 ID
     * @param reason 驳回原因
     */
    void reject(Long id, String reason);

    /**
     * 校验指定账号在指定角色下是否已有通过的入驻审核，未通过时抛出业务异常。
     *
     * @param accountId 账号 ID
     * @param role 账号角色
     */
    void assertApproved(Long accountId, AccountRoleEnum role);
}
