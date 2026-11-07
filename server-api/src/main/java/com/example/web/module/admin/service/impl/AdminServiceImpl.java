package com.example.web.module.admin.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.admin.dto.AdminCreateDTO;
import com.example.web.module.admin.dto.AdminQueryDTO;
import com.example.web.module.admin.dto.AdminResetPasswordDTO;
import com.example.web.module.admin.dto.AdminUpdateDTO;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
import com.example.web.module.admin.service.AdminService;
import com.example.web.module.admin.vo.AdminDetailVO;
import com.example.web.module.admin.vo.AdminListVO;
import com.example.web.module.auth.dto.AdminChangePasswordDTO;
import com.example.web.module.auth.dto.AdminProfileUpdateDTO;
import com.example.web.module.auth.vo.AdminProfileVO;
import com.example.web.security.LoginAccountUtil;
import com.example.web.util.PasswordUtil;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员业务实现，通过 accountId 组合统一账号和管理员资料。
 */
@Service
public class AdminServiceImpl implements AdminService {

    /**
     * 新增管理员的初始密码。
     */
    private static final String DEFAULT_PASSWORD = "123456789";

    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;

    public AdminServiceImpl(AccountMapper accountMapper, AdminMapper adminMapper) {
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
    }

    /**
     * 根据登录态中的账号 ID 查询当前管理员资料。
     *
     * @return 当前管理员展示信息
     */
    @Override
    public AdminProfileVO getCurrentAdminProfile() {
        Account account = getAuthorizedAdminAccount();
        return toProfileVO(account, getAdminByAccountId(account.getId()));
    }

    /**
     * 事务更新管理员姓名、手机号、邮箱和头像。
     *
     * @param dto 资料修改参数
     * @return 修改后的当前管理员信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AdminProfileVO updateCurrentAdminProfile(AdminProfileUpdateDTO dto) {
        Account account = getAuthorizedAdminAccount();
        Admin admin = getAdminByAccountId(account.getId());
        updateAccountStatus(account.getId(), account.getStatus());
        updateAdminProfile(admin.getId(), dto.getRealName(), dto.getPhone(), dto.getEmail(), dto.getAvatar());
        return toProfileVO(getAdminAccount(account.getId()), getExistingAdmin(admin.getId()));
    }

    /**
     * 校验原密码后只更新账号表中的密码凭据。
     *
     * @param dto 密码修改参数
     */
    @Override
    public void changeCurrentAdminPassword(AdminChangePasswordDTO dto) {
        Account account = getAuthorizedAdminAccount();
        if (!PasswordUtil.matches(dto.getOldPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        updatePassword(account.getId(), dto.getNewPassword());
        LoginAccountUtil.logout(account.getId());
    }

    /**
     * 按管理员角色分页查询账号，再通过 accountId 批量组合管理员资料。
     *
     * @param dto 查询条件和分页参数
     * @return 管理员分页展示数据
     */
    @Override
    public PageResult<AdminListVO> pageAdmins(AdminQueryDTO dto) {
        Page<Account> page = accountMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), buildPageWrapper(dto));
        Map<Long, Admin> adminMap = getAdminsByAccountIds(page.getRecords().stream().map(Account::getId).toList());
        return new PageResult<>(page.getRecords().stream()
                .map(account -> toListVO(account, adminMap.get(account.getId())))
                .toList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 按管理员资料 ID 查询详情。
     *
     * @param id 管理员资料 ID
     * @return 管理员详情
     */
    @Override
    public AdminDetailVO getAdminDetail(Long id) {
        Admin admin = getExistingAdmin(id);
        Account account = getAdminAccount(admin.getAccountId());
        AdminDetailVO vo = new AdminDetailVO();
        fillListVO(vo, account, admin);
        return vo;
    }

    /**
     * 事务创建账号和管理员资料，管理员 ID 独立自增并保存 accountId。
     *
     * @param dto 新增管理员参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createAdmin(AdminCreateDTO dto) {
        ensureUsernameAvailable(dto.getUsername());
        Account account = createAccount(dto.getUsername(), dto.getStatus());
        Admin admin = new Admin();
        admin.setAccountId(account.getId());
        admin.setRealName(dto.getRealName());
        admin.setPhone(dto.getPhone());
        admin.setEmail(dto.getEmail());
        admin.setAvatar(dto.getAvatar());
        adminMapper.insert(admin);
    }

    /**
     * 按管理员资料 ID 事务更新账号和管理员资料。
     *
     * @param dto 编辑管理员参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateAdmin(AdminUpdateDTO dto) {
        Admin admin = getExistingAdmin(dto.getId());
        EnableStatusEnum status = normalizeStatus(dto.getStatus());
        ensureAdminCanBeDisabled(admin, status);
        getAdminAccount(admin.getAccountId());
        updateAccountStatus(admin.getAccountId(), status);
        updateAdminProfile(admin.getId(), dto.getRealName(), dto.getPhone(), dto.getEmail(), dto.getAvatar());
        if (status == EnableStatusEnum.DISABLED) {
            LoginAccountUtil.logout(admin.getAccountId());
        }
    }

    /**
     * 按管理员资料 ID 删除管理员，并事务删除其关联账号。
     *
     * @param id 管理员资料 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteAdmin(Long id) {
        Admin admin = getExistingAdmin(id);
        ensureAdminCanBeDeleted(admin);
        getAdminAccount(admin.getAccountId());
        adminMapper.deleteById(id);
        accountMapper.deleteById(admin.getAccountId());
        LoginAccountUtil.logout(admin.getAccountId());
    }

    /**
     * 按管理员资料 ID 重置关联账号密码。
     *
     * @param dto 重置密码参数
     */
    @Override
    public void resetPassword(AdminResetPasswordDTO dto) {
        Admin admin = getExistingAdmin(dto.getId());
        getAdminAccount(admin.getAccountId());
        updatePassword(admin.getAccountId(), dto.getNewPassword());
        LoginAccountUtil.logout(admin.getAccountId());
    }

    /**
     * 校验管理员是否允许删除，禁止删除当前登录账号和最后一个管理员。
     *
     * @param admin 待删除管理员
     */
    private void ensureAdminCanBeDeleted(Admin admin) {
        if (admin.getAccountId().equals(LoginAccountUtil.getCurrentAccountId())) {
            throw new BusinessException("不能删除当前登录的管理员账号");
        }
        long adminCount = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getRole, AccountRoleEnum.ADMIN));
        if (adminCount <= 1) {
            throw new BusinessException("至少保留一个管理员账号");
        }
    }

    /**
     * 校验管理员是否允许禁用，禁止禁用当前登录账号和最后一个启用的管理员。
     *
     * @param admin 待编辑管理员
     * @param targetStatus 目标状态
     */
    private void ensureAdminCanBeDisabled(Admin admin, EnableStatusEnum targetStatus) {
        if (targetStatus != EnableStatusEnum.DISABLED) {
            return;
        }
        Account account = getAdminAccount(admin.getAccountId());
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            return;
        }
        if (admin.getAccountId().equals(LoginAccountUtil.getCurrentAccountId())) {
            throw new BusinessException("不能禁用当前登录的管理员账号");
        }
        long enabledCount = accountMapper.selectCount(new LambdaQueryWrapper<Account>()
                .eq(Account::getRole, AccountRoleEnum.ADMIN)
                .eq(Account::getStatus, EnableStatusEnum.ENABLED));
        if (enabledCount <= 1) {
            throw new BusinessException("至少保留一个启用的管理员账号");
        }
    }

    private Account createAccount(String username, EnableStatusEnum status) {
        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(AccountRoleEnum.ADMIN);
        account.setUsername(username);
        account.setStatus(normalizeStatus(status));
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(DEFAULT_PASSWORD, salt));
        accountMapper.insert(account);
        return account;
    }

    private void ensureUsernameAvailable(String username) {
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getUsername, username)) > 0) {
            throw new BusinessException("账号已存在");
        }
    }

    private Account getAuthorizedAdminAccount() {
        Account account = getAdminAccount(LoginAccountUtil.getCurrentAccountId());
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已禁用");
        }
        return account;
    }

    private Account getAdminAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || account.getRole() != AccountRoleEnum.ADMIN) {
            throw new BusinessException("管理员账号不存在");
        }
        return account;
    }

    private Admin getExistingAdmin(Long id) {
        Admin admin = adminMapper.selectById(id);
        if (admin == null) {
            throw new BusinessException("管理员不存在");
        }
        return admin;
    }

    private Admin getAdminByAccountId(Long accountId) {
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getAccountId, accountId));
        if (admin == null) {
            throw new BusinessException("管理员资料不存在");
        }
        return admin;
    }

    private LambdaQueryWrapper<Account> buildPageWrapper(AdminQueryDTO dto) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getRole, AccountRoleEnum.ADMIN);
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            List<Long> accountIds = adminMapper.selectList(new LambdaQueryWrapper<Admin>()
                            .like(Admin::getRealName, dto.getKeyword()))
                    .stream().map(Admin::getAccountId).toList();
            wrapper.and(item -> {
                item.like(Account::getUsername, dto.getKeyword());
                if (!accountIds.isEmpty()) {
                    item.or().in(Account::getId, accountIds);
                }
            });
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Account::getStatus, dto.getStatus());
        }
        return wrapper.orderByDesc(Account::getId);
    }

    private Map<Long, Admin> getAdminsByAccountIds(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return adminMapper.selectList(new LambdaQueryWrapper<Admin>().in(Admin::getAccountId, accountIds)).stream()
                .collect(Collectors.toMap(Admin::getAccountId, Function.identity()));
    }

    private void updateAccountStatus(Long accountId, EnableStatusEnum status) {
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getStatus, status)
                .eq(Account::getId, accountId));
    }

    private void updateAdminProfile(Long id, String realName, String phone, String email, String avatar) {
        adminMapper.update(null, new LambdaUpdateWrapper<Admin>()
                .set(Admin::getRealName, realName)
                .set(Admin::getPhone, phone)
                .set(Admin::getEmail, email)
                .set(Admin::getAvatar, avatar)
                .eq(Admin::getId, id));
    }

    private void updatePassword(Long accountId, String newPassword) {
        String salt = PasswordUtil.generateSalt();
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getPasswordSalt, salt)
                .set(Account::getPasswordHash, PasswordUtil.hashPassword(newPassword, salt))
                .eq(Account::getId, accountId));
    }

    private EnableStatusEnum normalizeStatus(EnableStatusEnum status) {
        return status == null ? EnableStatusEnum.ENABLED : status;
    }

    private AdminListVO toListVO(Account account, Admin admin) {
        AdminListVO vo = new AdminListVO();
        fillListVO(vo, account, admin);
        return vo;
    }

    private AdminProfileVO toProfileVO(Account account, Admin admin) {
        AdminProfileVO vo = new AdminProfileVO();
        vo.setId(admin.getId());
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setRealName(admin.getRealName());
        vo.setPhone(admin.getPhone());
        vo.setEmail(admin.getEmail());
        vo.setAvatar(admin.getAvatar());
        vo.setClientType(account.getRole().getClientType());
        return vo;
    }

    private void fillListVO(AdminListVO vo, Account account, Admin admin) {
        vo.setId(admin == null ? null : admin.getId());
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setRealName(admin == null ? null : admin.getRealName());
        vo.setPhone(admin == null ? null : admin.getPhone());
        vo.setEmail(admin == null ? null : admin.getEmail());
        vo.setAvatar(admin == null ? null : admin.getAvatar());
        vo.setStatus(account.getStatus());
        vo.setLastLoginTime(account.getLastLoginTime());
        vo.setCreateTime(admin == null ? null : admin.getCreateTime());
    }
}
