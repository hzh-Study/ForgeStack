package com.example.web.module.auth.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
import com.example.web.module.auth.dto.LoginDTO;
import com.example.web.module.auth.dto.PasswordResetDTO;
import com.example.web.module.auth.dto.PasswordResetVerifyDTO;
import com.example.web.module.auth.dto.RegisterDTO;
import com.example.web.module.auth.service.AuthService;
import com.example.web.module.auth.vo.LoginVO;
import com.example.web.module.auth.vo.PasswordResetVerifyVO;
import com.example.web.module.user.entity.User;
import com.example.web.module.user.mapper.UserMapper;
import com.example.web.security.LoginAccountUtil;
import com.example.web.util.PasswordUtil;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统一认证业务实现，所有角色只在账号表校验登录凭据。
 */
@Service
public class AuthServiceImpl implements AuthService {

    /**
     * 重置密码凭证有效时间，单位为秒。
     */
    private static final long RESET_TOKEN_TTL_SECONDS = 600L;

    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;
    private final UserMapper userMapper;

    /**
     * 一次性重置凭证仅保存在服务内存中，使用后立即删除。
     */
    private final Map<String, PasswordResetTicket> passwordResetTickets = new ConcurrentHashMap<>();

    public AuthServiceImpl(AccountMapper accountMapper, AdminMapper adminMapper, UserMapper userMapper) {
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
        this.userMapper = userMapper;
    }

    /**
     * 校验全局唯一账号、状态和密码，并用账号 ID 建立登录态。
     *
     * @param dto 登录账号和密码
     * @return Token、角色端侧和账号信息
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, dto.getUsername()));
        if (account == null || !PasswordUtil.matches(dto.getPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            throw new BusinessException("账号或密码错误");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已禁用");
        }
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getLastLoginTime, LocalDateTime.now())
                .eq(Account::getId, account.getId()));
        LoginAccountUtil.login(account.getId());
        return new LoginVO(LoginAccountUtil.getTokenValue(), account.getRole().getClientType(), account.getUsername());
    }

    /**
     * 事务创建前台用户账号和用户资料，用户资料通过 accountId 关联账号。
     *
     * @param dto 注册参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerUser(RegisterDTO dto) {
        String username = dto.getUsername().trim();
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getUsername, username)) > 0) {
            throw new BusinessException("账号已存在");
        }

        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(AccountRoleEnum.USER);
        account.setUsername(username);
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(dto.getPassword(), salt));
        account.setStatus(EnableStatusEnum.ENABLED);
        accountMapper.insert(account);

        User user = new User();
        user.setAccountId(account.getId());
        user.setNickname(dto.getNickname().trim());
        user.setEmail(dto.getEmail().trim());
        userMapper.insert(user);
    }

    /**
     * 校验账号角色及其资料表邮箱，成功后签发十分钟内有效的一次性凭证。
     *
     * @param dto 身份校验参数
     * @return 一次性重置凭证
     */
    @Override
    public PasswordResetVerifyVO verifyPasswordReset(PasswordResetVerifyDTO dto) {
        removeExpiredTickets();
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, dto.getUsername().trim()));
        if (account == null || account.getRole() != dto.getRole()) {
            throw new BusinessException("账号或邮箱不匹配");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已禁用");
        }

        String profileEmail = getProfileEmail(account);
        if (profileEmail == null || !profileEmail.equalsIgnoreCase(dto.getEmail().trim())) {
            throw new BusinessException("账号或邮箱不匹配");
        }

        String resetToken = UUID.randomUUID().toString().replace("-", "");
        passwordResetTickets.put(resetToken,
                new PasswordResetTicket(account.getId(), Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS)));
        return new PasswordResetVerifyVO(resetToken, RESET_TOKEN_TTL_SECONDS);
    }

    /**
     * 消费一次性重置凭证并更新账号表密码，凭证无论成功与否都不能重复使用。
     *
     * @param dto 重置密码参数
     */
    @Override
    public void resetPassword(PasswordResetDTO dto) {
        PasswordResetTicket ticket = passwordResetTickets.remove(dto.getResetToken());
        if (ticket == null || ticket.expiresAt().isBefore(Instant.now())) {
            throw new BusinessException("重置凭证无效或已过期，请重新验证");
        }
        if (accountMapper.selectById(ticket.accountId()) == null) {
            throw new BusinessException("账号不存在");
        }

        String salt = PasswordUtil.generateSalt();
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getPasswordSalt, salt)
                .set(Account::getPasswordHash, PasswordUtil.hashPassword(dto.getNewPassword(), salt))
                .eq(Account::getId, ticket.accountId()));
    }

    /**
     * 按账号角色从对应资料表读取邮箱，账号表不保存角色资料。
     *
     * @param account 统一账号
     * @return 角色资料邮箱
     */
    private String getProfileEmail(Account account) {
        return switch (account.getRole()) {
            case ADMIN -> {
                Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccountId, account.getId()));
                yield admin == null ? null : admin.getEmail();
            }
            case USER -> {
                User user = userMapper.selectOne(new LambdaQueryWrapper<User>()
                        .eq(User::getAccountId, account.getId()));
                yield user == null ? null : user.getEmail();
            }
        };
    }

    /**
     * 清理已过期凭证，避免长期占用服务内存。
     */
    private void removeExpiredTickets() {
        Instant now = Instant.now();
        passwordResetTickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
    }

    /**
     * 一次性密码重置凭证记录。
     *
     * @param accountId 账号 ID
     * @param expiresAt 过期时间
     */
    private record PasswordResetTicket(Long accountId, Instant expiresAt) {
    }
}
