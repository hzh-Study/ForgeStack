package com.example.web.module.account.service;

import com.example.web.common.page.PageResult;
import com.example.web.module.account.dto.AccountQueryDTO;
import com.example.web.module.account.dto.AdminAccountCreateDTO;
import com.example.web.module.account.vo.AccountListVO;

/**
 * 管理员账号治理业务接口，覆盖四类账号查询与停用启用。
 */
public interface AccountGovernanceService {

    /**
     * 创建管理员账号与管理员资料，手机号即登录账号。
     *
     * @param dto 创建参数
     * @return 新建账号 ID
     */
    Long createAdminAccount(AdminAccountCreateDTO dto);

    /**
     * 分页查询四类账号。
     *
     * @param dto 查询参数
     * @return 账号分页数据
     */
    PageResult<AccountListVO> pageAccounts(AccountQueryDTO dto);

    /**
     * 停用指定账号并立即使其登录态失效。
     *
     * @param accountId 账号 ID
     */
    void disableAccount(Long accountId);

    /**
     * 启用指定账号。
     *
     * @param accountId 账号 ID
     */
    void enableAccount(Long accountId);
}
