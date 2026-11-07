package com.example.web.security;

import cn.dev33.satoken.stp.StpUtil;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.mapper.AccountMapper;
import org.springframework.stereotype.Component;

/**
 * 业务接口访问守卫，在登录校验之后统一拦截停用账号与未完成初始改密的司机。
 */
@Component
public class BusinessAccessGuard {

    private final AccountMapper accountMapper;

    public BusinessAccessGuard(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    /**
     * 校验当前登录账号状态与改密要求。
     */
    public void check() {
        String loginId = StpUtil.getLoginIdAsString();
        Long accountId = LoginAccountUtil.parseAccountId(loginId);
        Account account = accountMapper.selectById(accountId);
        if (account == null || account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已停用，请联系平台管理员");
        }
        if (Integer.valueOf(1).equals(account.getMustChangePassword()) && account.getRole() != null
                && !"ADMIN".equals(account.getRole().getCode())) {
            throw new BusinessException("请先修改初始密码后再进行业务操作");
        }
    }
}
