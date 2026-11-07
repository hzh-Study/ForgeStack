package com.example.web.module.user.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.vo.UserProfileVO;
import com.example.web.module.user.dto.UserCreateDTO;
import com.example.web.module.user.dto.UserQueryDTO;
import com.example.web.module.user.dto.UserResetPasswordDTO;
import com.example.web.module.user.dto.UserUpdateDTO;
import com.example.web.module.user.entity.User;
import com.example.web.module.user.mapper.UserMapper;
import com.example.web.module.user.service.UserService;
import com.example.web.module.user.vo.UserDetailVO;
import com.example.web.module.user.vo.UserListVO;
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
 * 前台用户业务实现，通过 accountId 组合统一账号和用户资料。
 */
@Service
public class UserServiceImpl implements UserService {

    /**
     * 新增用户的初始密码。
     */
    private static final String DEFAULT_PASSWORD = "123456789";

    private final AccountMapper accountMapper;
    private final UserMapper userMapper;

    public UserServiceImpl(AccountMapper accountMapper, UserMapper userMapper) {
        this.accountMapper = accountMapper;
        this.userMapper = userMapper;
    }

    /**
     * 根据登录态中的账号 ID 查询当前用户资料。
     *
     * @return 当前用户展示信息
     */
    @Override
    public UserProfileVO getCurrentUserProfile() {
        Account account = getAuthorizedUserAccount();
        return toProfileVO(account, getUserByAccountId(account.getId()));
    }

    /**
     * 事务更新用户昵称、手机号、邮箱和头像。
     *
     * @param dto 资料修改参数
     * @return 修改后的当前用户信息
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public UserProfileVO updateCurrentUserProfile(UserProfileUpdateDTO dto) {
        Account account = getAuthorizedUserAccount();
        User user = getUserByAccountId(account.getId());
        updateAccountStatus(account.getId(), account.getStatus());
        updateUserProfile(user.getId(), dto.getNickname(), dto.getPhone(), dto.getEmail(), dto.getAvatar());
        return toProfileVO(getUserAccount(account.getId()), getExistingUser(user.getId()));
    }

    /**
     * 校验原密码后只更新账号表中的密码凭据。
     *
     * @param dto 密码修改参数
     */
    @Override
    public void changeCurrentUserPassword(UserChangePasswordDTO dto) {
        Account account = getAuthorizedUserAccount();
        if (!PasswordUtil.matches(dto.getOldPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            throw new BusinessException("原密码错误");
        }
        updatePassword(account.getId(), dto.getNewPassword());
        LoginAccountUtil.logout(account.getId());
    }

    /**
     * 按用户角色分页查询账号，再通过 accountId 批量组合用户资料。
     *
     * @param dto 查询条件和分页参数
     * @return 用户分页展示数据
     */
    @Override
    public PageResult<UserListVO> pageUsers(UserQueryDTO dto) {
        Page<Account> page = accountMapper.selectPage(
                new Page<>(dto.getPageNum(), dto.getPageSize()), buildPageWrapper(dto));
        Map<Long, User> userMap = getUsersByAccountIds(page.getRecords().stream().map(Account::getId).toList());
        return new PageResult<>(page.getRecords().stream()
                .map(account -> toListVO(account, userMap.get(account.getId())))
                .toList(), page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 按用户资料 ID 查询详情。
     *
     * @param id 用户资料 ID
     * @return 用户详情
     */
    @Override
    public UserDetailVO getUserDetail(Long id) {
        User user = getExistingUser(id);
        Account account = getUserAccount(user.getAccountId());
        UserDetailVO vo = new UserDetailVO();
        fillListVO(vo, account, user);
        return vo;
    }

    /**
     * 事务创建账号和用户资料，用户 ID 独立自增并保存 accountId。
     *
     * @param dto 新增用户参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void createUser(UserCreateDTO dto) {
        ensureUsernameAvailable(dto.getUsername());
        Account account = createAccount(dto.getUsername(), dto.getStatus());
        User user = new User();
        user.setAccountId(account.getId());
        user.setNickname(dto.getNickname());
        user.setPhone(dto.getPhone());
        user.setEmail(dto.getEmail());
        user.setAvatar(dto.getAvatar());
        userMapper.insert(user);
    }

    /**
     * 按用户资料 ID 事务更新账号和用户资料。
     *
     * @param dto 编辑用户参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateUser(UserUpdateDTO dto) {
        User user = getExistingUser(dto.getId());
        EnableStatusEnum status = normalizeStatus(dto.getStatus());
        getUserAccount(user.getAccountId());
        updateAccountStatus(user.getAccountId(), status);
        updateUserProfile(user.getId(), dto.getNickname(), dto.getPhone(), dto.getEmail(), dto.getAvatar());
        if (status == EnableStatusEnum.DISABLED) {
            LoginAccountUtil.logout(user.getAccountId());
        }
    }

    /**
     * 按用户资料 ID 删除用户，并事务删除其关联账号。
     *
     * @param id 用户资料 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteUser(Long id) {
        User user = getExistingUser(id);
        getUserAccount(user.getAccountId());
        userMapper.deleteById(id);
        accountMapper.deleteById(user.getAccountId());
        LoginAccountUtil.logout(user.getAccountId());
    }

    /**
     * 按用户资料 ID 重置关联账号密码。
     *
     * @param dto 重置密码参数
     */
    @Override
    public void resetPassword(UserResetPasswordDTO dto) {
        User user = getExistingUser(dto.getId());
        getUserAccount(user.getAccountId());
        updatePassword(user.getAccountId(), dto.getNewPassword());
        LoginAccountUtil.logout(user.getAccountId());
    }

    private Account createAccount(String username, EnableStatusEnum status) {
        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(AccountRoleEnum.USER);
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

    private Account getAuthorizedUserAccount() {
        Account account = getUserAccount(LoginAccountUtil.getCurrentAccountId());
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已禁用");
        }
        return account;
    }

    private Account getUserAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null || account.getRole() != AccountRoleEnum.USER) {
            throw new BusinessException("用户账号不存在");
        }
        return account;
    }

    private User getExistingUser(Long id) {
        User user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException("用户不存在");
        }
        return user;
    }

    private User getUserByAccountId(Long accountId) {
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getAccountId, accountId));
        if (user == null) {
            throw new BusinessException("用户资料不存在");
        }
        return user;
    }

    private LambdaQueryWrapper<Account> buildPageWrapper(UserQueryDTO dto) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<Account>()
                .eq(Account::getRole, AccountRoleEnum.USER);
        if (dto.getKeyword() != null && !dto.getKeyword().isBlank()) {
            List<Long> accountIds = userMapper.selectList(new LambdaQueryWrapper<User>()
                            .like(User::getNickname, dto.getKeyword()))
                    .stream().map(User::getAccountId).toList();
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

    private Map<Long, User> getUsersByAccountIds(List<Long> accountIds) {
        if (accountIds.isEmpty()) {
            return Collections.emptyMap();
        }
        return userMapper.selectList(new LambdaQueryWrapper<User>().in(User::getAccountId, accountIds)).stream()
                .collect(Collectors.toMap(User::getAccountId, Function.identity()));
    }

    private void updateAccountStatus(Long accountId, EnableStatusEnum status) {
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getStatus, status)
                .eq(Account::getId, accountId));
    }

    private void updateUserProfile(Long id, String nickname, String phone, String email, String avatar) {
        userMapper.update(null, new LambdaUpdateWrapper<User>()
                .set(User::getNickname, nickname)
                .set(User::getPhone, phone)
                .set(User::getEmail, email)
                .set(User::getAvatar, avatar)
                .eq(User::getId, id));
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

    private UserListVO toListVO(Account account, User user) {
        UserListVO vo = new UserListVO();
        fillListVO(vo, account, user);
        return vo;
    }

    private UserProfileVO toProfileVO(Account account, User user) {
        UserProfileVO vo = new UserProfileVO();
        vo.setId(user.getId());
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setNickname(user.getNickname());
        vo.setPhone(user.getPhone());
        vo.setEmail(user.getEmail());
        vo.setAvatar(user.getAvatar());
        vo.setClientType(account.getRole().getClientType());
        return vo;
    }

    private void fillListVO(UserListVO vo, Account account, User user) {
        vo.setId(user == null ? null : user.getId());
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setNickname(user == null ? null : user.getNickname());
        vo.setPhone(user == null ? null : user.getPhone());
        vo.setEmail(user == null ? null : user.getEmail());
        vo.setAvatar(user == null ? null : user.getAvatar());
        vo.setStatus(account.getStatus());
        vo.setCreateTime(user == null ? null : user.getCreateTime());
    }
}
