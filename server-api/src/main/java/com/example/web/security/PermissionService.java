package com.example.web.security;

import cn.dev33.satoken.stp.StpInterface;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Sa-Token 权限加载服务，根据统一账号表中的角色分配端侧权限。
 */
@Component
public class PermissionService implements StpInterface {

    private final AccountMapper accountMapper;

    public PermissionService(AccountMapper accountMapper) {
        this.accountMapper = accountMapper;
    }

    /**
     * 按账号角色加载权限码。
     *
     * @param loginId 登录 ID
     * @param loginType 登录类型
     * @return 权限码列表
     */
    @Override
    public List<String> getPermissionList(Object loginId, String loginType) {
        AccountRoleEnum role = getAccountRole(loginId);
        return role == AccountRoleEnum.ADMIN ? getAdminPermissions() : getBusinessPermissions();
    }

    /**
     * 返回统一账号表中的角色码。
     *
     * @param loginId 登录 ID
     * @param loginType 登录类型
     * @return 角色码列表
     */
    @Override
    public List<String> getRoleList(Object loginId, String loginType) {
        return List.of(getAccountRole(loginId).getCode());
    }

    private AccountRoleEnum getAccountRole(Object loginId) {
        Long accountId = LoginAccountUtil.parseAccountId(loginId);
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException("登录已失效");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已禁用");
        }
        return account.getRole();
    }

    private List<String> getAdminPermissions() {
        return List.of(
                PermissionCode.ADMIN_PROFILE,
                PermissionCode.ADMIN_PROFILE_UPDATE,
                PermissionCode.ADMIN_PASSWORD_UPDATE,
                PermissionCode.ADMIN_FILE_UPLOAD,
                PermissionCode.ADMIN_LIST,
                PermissionCode.ADMIN_DETAIL,
                PermissionCode.ADMIN_CREATE,
                PermissionCode.ADMIN_UPDATE,
                PermissionCode.ADMIN_DELETE,
                PermissionCode.ADMIN_RESET_PASSWORD,
                PermissionCode.USER_MANAGE_LIST,
                PermissionCode.USER_MANAGE_DETAIL,
                PermissionCode.USER_MANAGE_CREATE,
                PermissionCode.USER_MANAGE_UPDATE,
                PermissionCode.USER_MANAGE_DELETE,
                PermissionCode.USER_MANAGE_RESET_PASSWORD
        );
    }

    private List<String> getBusinessPermissions() {
        return List.of(
                PermissionCode.WEB_PROFILE,
                PermissionCode.WEB_PROFILE_UPDATE,
                PermissionCode.WEB_PASSWORD_UPDATE,
                PermissionCode.WEB_FILE_UPLOAD,
                PermissionCode.BUSINESS_PROFILE,
                PermissionCode.BUSINESS_PROFILE_UPDATE,
                PermissionCode.BUSINESS_PASSWORD_UPDATE,
                PermissionCode.BUSINESS_FILE_UPLOAD
        );
    }
}
