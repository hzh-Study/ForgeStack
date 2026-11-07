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
import com.example.web.module.auth.dto.PasswordResetCodeDTO;
import com.example.web.module.auth.dto.PasswordResetDTO;
import com.example.web.module.auth.dto.PasswordResetVerifyDTO;
import com.example.web.module.auth.dto.RegisterDTO;
import com.example.web.module.auth.dto.UserChangePasswordDTO;
import com.example.web.module.auth.dto.UserProfileUpdateDTO;
import com.example.web.module.auth.service.AuthService;
import com.example.web.module.auth.vo.AccountProfileVO;
import com.example.web.module.auth.vo.LoginVO;
import com.example.web.module.auth.vo.PasswordResetVerifyVO;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.onboarding.entity.Onboarding;
import com.example.web.module.onboarding.mapper.OnboardingMapper;
import com.example.web.module.realname.entity.Realname;
import com.example.web.module.realname.mapper.RealnameMapper;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.module.vehicle.entity.Vehicle;
import com.example.web.module.vehicle.mapper.VehicleMapper;
import com.example.web.security.CurrentAccountService;
import com.example.web.security.LoginAccountUtil;
import com.example.web.util.PasswordUtil;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 统一认证业务实现，四类角色只在账号表校验登录凭据。
 */
@Service
public class AuthServiceImpl implements AuthService {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthServiceImpl.class);

    /**
     * 重置密码凭证有效时间，单位为秒。
     */
    private static final long RESET_TOKEN_TTL_SECONDS = 600L;

    /**
     * 邮箱验证码有效时间，单位为秒。
     */
    private static final long RESET_CODE_TTL_SECONDS = 600L;

    /**
     * 同一账号重新发送验证码的最小间隔，单位为秒。
     */
    private static final long RESET_CODE_COOLDOWN_SECONDS = 60L;

    /**
     * 单个验证码允许的最大错误次数。
     */
    private static final int RESET_CODE_MAX_ATTEMPTS = 5;

    private static final SecureRandom RANDOM = new SecureRandom();

    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;
    private final ShipperMapper shipperMapper;
    private final LogisticsMapper logisticsMapper;
    private final DriverMapper driverMapper;
    private final RealnameMapper realnameMapper;
    private final OnboardingMapper onboardingMapper;
    private final VehicleMapper vehicleMapper;
    private final CurrentAccountService currentAccountService;

    /**
     * 一次性重置凭证仅保存在服务内存中，使用后立即删除。
     */
    private final Map<String, PasswordResetTicket> passwordResetTickets = new ConcurrentHashMap<>();

    /**
     * 邮箱验证码按账号 ID 保存在服务内存中，验证成功或过期后删除。
     */
    private final Map<Long, PasswordResetCodeTicket> passwordResetCodes = new ConcurrentHashMap<>();

    public AuthServiceImpl(AccountMapper accountMapper, AdminMapper adminMapper, ShipperMapper shipperMapper,
                           LogisticsMapper logisticsMapper, DriverMapper driverMapper, RealnameMapper realnameMapper,
                           OnboardingMapper onboardingMapper, VehicleMapper vehicleMapper,
                           CurrentAccountService currentAccountService) {
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
        this.shipperMapper = shipperMapper;
        this.logisticsMapper = logisticsMapper;
        this.driverMapper = driverMapper;
        this.realnameMapper = realnameMapper;
        this.onboardingMapper = onboardingMapper;
        this.vehicleMapper = vehicleMapper;
        this.currentAccountService = currentAccountService;
    }

    /**
     * 校验统一账号、状态和密码，并用账号 ID 建立登录态。
     *
     * @param dto 手机号与密码
     * @return Token、角色与端侧信息
     */
    @Override
    public LoginVO login(LoginDTO dto) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, dto.getUsername().trim()));
        if (account == null || !PasswordUtil.matches(dto.getPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            throw new BusinessException("手机号或密码错误");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已停用，请联系平台管理员");
        }
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getLastLoginTime, LocalDateTime.now())
                .eq(Account::getId, account.getId()));
        LoginAccountUtil.login(account.getId());
        AccountRoleEnum role = account.getRole();
        return new LoginVO(LoginAccountUtil.getTokenValue(), role.getClientType(), account.getUsername(),
                role.getCode(), account.getId(), getDisplayName(account),
                Integer.valueOf(1).equals(account.getMustChangePassword()));
    }

    /**
     * 事务创建货主或物流公司账号与角色资料。
     *
     * @param dto 注册参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void registerBusiness(RegisterDTO dto) {
        AccountRoleEnum role = dto.getRole();
        if (role != AccountRoleEnum.SHIPPER && role != AccountRoleEnum.LOGISTICS) {
            throw new BusinessException("只允许货主或物流公司自助注册");
        }
        String phone = dto.getPhone().trim();
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getUsername, phone)) > 0) {
            throw new BusinessException("手机号已注册");
        }
        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(role);
        account.setUsername(phone);
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(dto.getPassword(), salt));
        account.setStatus(EnableStatusEnum.ENABLED);
        account.setMustChangePassword(0);
        accountMapper.insert(account);

        if (role == AccountRoleEnum.SHIPPER) {
            Shipper shipper = new Shipper();
            shipper.setAccountId(account.getId());
            shipper.setName(dto.getName().trim());
            shipper.setPhone(phone);
            shipper.setEmail(dto.getEmail());
            shipperMapper.insert(shipper);
        } else {
            Logistics logistics = new Logistics();
            logistics.setAccountId(account.getId());
            logistics.setName(dto.getName().trim());
            logistics.setPhone(phone);
            logistics.setEmail(dto.getEmail());
            logisticsMapper.insert(logistics);
        }
    }

    /**
     * 查询当前登录账号完整资料。
     *
     * @return 当前账号资料
     */
    @Override
    public AccountProfileVO getCurrentProfile() {
        Account account = currentAccountService.getAccount();
        return buildProfile(account);
    }

    /**
     * 修改当前登录账号展示资料。
     *
     * @param dto 资料修改参数
     * @return 修改后的账号资料
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public AccountProfileVO updateCurrentProfile(UserProfileUpdateDTO dto) {
        Account account = currentAccountService.getAccount();
        String name = dto.getName() == null || dto.getName().isBlank() ? null : dto.getName().trim();
        switch (account.getRole()) {
            case ADMIN -> {
                Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccountId, account.getId()));
                if (admin == null) {
                    throw new BusinessException("管理员资料不存在");
                }
                if (name != null) {
                    admin.setRealName(name);
                }
                admin.setEmail(dto.getEmail());
                admin.setAvatar(dto.getAvatar());
                adminMapper.updateById(admin);
            }
            case SHIPPER -> {
                Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                        .eq(Shipper::getAccountId, account.getId()));
                if (shipper == null) {
                    throw new BusinessException("货主资料不存在");
                }
                if (name != null) {
                    shipper.setName(name);
                }
                shipper.setEmail(dto.getEmail());
                shipper.setAvatar(dto.getAvatar());
                shipperMapper.updateById(shipper);
            }
            case LOGISTICS -> {
                Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, account.getId()));
                if (logistics == null) {
                    throw new BusinessException("物流公司资料不存在");
                }
                if (name != null) {
                    logistics.setName(name);
                }
                logistics.setEmail(dto.getEmail());
                logistics.setAvatar(dto.getAvatar());
                logisticsMapper.updateById(logistics);
            }
            case DRIVER -> {
                Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getAccountId, account.getId()));
                if (driver == null) {
                    throw new BusinessException("司机资料不存在");
                }
                if (name != null) {
                    driver.setName(name);
                }
                driver.setEmail(dto.getEmail());
                driver.setAvatar(dto.getAvatar());
                driverMapper.updateById(driver);
            }
            default -> throw new BusinessException("当前角色不支持修改资料");
        }
        return buildProfile(account);
    }

    /**
     * 修改当前账号密码，成功后清理全部登录态。
     *
     * @param dto 密码修改参数
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void changeCurrentPassword(UserChangePasswordDTO dto) {
        Account account = currentAccountService.getAccount();
        if (!PasswordUtil.matches(dto.getOldPassword(), account.getPasswordSalt(), account.getPasswordHash())) {
            throw new BusinessException("原密码不正确");
        }
        if (dto.getNewPassword().equals(dto.getOldPassword())) {
            throw new BusinessException("新密码不能与原密码相同");
        }
        String salt = PasswordUtil.generateSalt();
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getPasswordSalt, salt)
                .set(Account::getPasswordHash, PasswordUtil.hashPassword(dto.getNewPassword(), salt))
                .set(Account::getMustChangePassword, 0)
                .eq(Account::getId, account.getId()));
        LoginAccountUtil.logout(account.getId());
    }

    @Override
    public void sendPasswordResetCode(PasswordResetCodeDTO dto) {
        removeExpiredTickets();
        Account account = getAccountForPasswordReset(dto.getUsername(), dto.getEmail(), dto.getRole());
        Instant now = Instant.now();
        PasswordResetCodeTicket existing = passwordResetCodes.get(account.getId());
        if (existing != null && existing.getResendAt().isAfter(now)) {
            throw new BusinessException("验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        passwordResetCodes.put(account.getId(), new PasswordResetCodeTicket(
                code,
                now.plusSeconds(RESET_CODE_TTL_SECONDS),
                now.plusSeconds(RESET_CODE_COOLDOWN_SECONDS)));
        LOGGER.info("[DEV] password reset code: account={}, code={}, ttl={}s",
                account.getUsername(), code, RESET_CODE_TTL_SECONDS);
    }

    @Override
    public PasswordResetVerifyVO verifyPasswordReset(PasswordResetVerifyDTO dto) {
        removeExpiredTickets();
        Account account = getAccountForPasswordReset(dto.getUsername(), dto.getEmail(), dto.getRole());
        PasswordResetCodeTicket codeTicket = passwordResetCodes.get(account.getId());
        if (codeTicket == null || codeTicket.getExpiresAt().isBefore(Instant.now())) {
            throw new BusinessException("验证码已过期，请重新获取");
        }
        if (!codeTicket.getCode().equals(dto.getCode())) {
            codeTicket.increaseFailedAttempts();
            if (codeTicket.getFailedAttempts() >= RESET_CODE_MAX_ATTEMPTS) {
                passwordResetCodes.remove(account.getId());
                throw new BusinessException("验证码错误次数过多，请重新获取");
            }
            throw new BusinessException("验证码错误");
        }

        passwordResetCodes.remove(account.getId());
        String resetToken = UUID.randomUUID().toString().replace("-", "");
        passwordResetTickets.put(resetToken,
                new PasswordResetTicket(account.getId(), Instant.now().plusSeconds(RESET_TOKEN_TTL_SECONDS)));
        return new PasswordResetVerifyVO(resetToken, RESET_TOKEN_TTL_SECONDS);
    }

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
                .set(Account::getMustChangePassword, 0)
                .eq(Account::getId, ticket.accountId()));
        LoginAccountUtil.logout(ticket.accountId());
    }

    /**
     * 按角色组装当前账号资料，并补齐实名、入驻与司机绑定信息。
     *
     * @param account 当前账号
     * @return 账号资料
     */
    private AccountProfileVO buildProfile(Account account) {
        AccountProfileVO vo = new AccountProfileVO();
        vo.setAccountId(account.getId());
        vo.setUsername(account.getUsername());
        vo.setRole(account.getRole().getCode());
        vo.setRoleName(account.getRole().getName());
        vo.setClientType(account.getRole().getClientType());
        vo.setMustChangePassword(Integer.valueOf(1).equals(account.getMustChangePassword()));

        Realname realname = getLatestRealname(account.getId());
        if (realname != null) {
            vo.setRealnameStatus(realname.getVerifyStatus());
            vo.setRealnameFailReason(realname.getFailReason());
        }

        switch (account.getRole()) {
            case ADMIN -> {
                Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccountId, account.getId()));
                if (admin != null) {
                    vo.setName(admin.getRealName());
                    vo.setEmail(admin.getEmail());
                    vo.setAvatar(admin.getAvatar());
                }
            }
            case SHIPPER -> {
                Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                        .eq(Shipper::getAccountId, account.getId()));
                if (shipper != null) {
                    vo.setName(shipper.getName());
                    vo.setEmail(shipper.getEmail());
                    vo.setAvatar(shipper.getAvatar());
                }
            }
            case LOGISTICS -> {
                Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, account.getId()));
                if (logistics != null) {
                    vo.setName(logistics.getName());
                    vo.setEmail(logistics.getEmail());
                    vo.setAvatar(logistics.getAvatar());
                }
            }
            case DRIVER -> {
                Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getAccountId, account.getId()));
                if (driver != null) {
                    vo.setName(driver.getName());
                    vo.setEmail(driver.getEmail());
                    vo.setAvatar(driver.getAvatar());
                    vo.setDriverId(driver.getId());
                    vo.setLogisticsId(driver.getLogisticsId());
                    vo.setVehicleId(driver.getVehicleId());
                    Logistics logistics = logisticsMapper.selectById(driver.getLogisticsId());
                    if (logistics != null) {
                        vo.setLogisticsName(logistics.getName());
                    }
                    if (driver.getVehicleId() != null) {
                        Vehicle vehicle = vehicleMapper.selectById(driver.getVehicleId());
                        if (vehicle != null) {
                            vo.setVehiclePlateNo(vehicle.getPlateNo());
                        }
                    }
                }
            }
            default -> {
            }
        }

        if (account.getRole() == AccountRoleEnum.SHIPPER || account.getRole() == AccountRoleEnum.LOGISTICS) {
            Onboarding onboarding = getLatestOnboarding(account.getId());
            if (onboarding != null) {
                vo.setOnboardingStatus(onboarding.getAuditStatus());
                vo.setOnboardingRejectReason(onboarding.getRejectReason());
            }
        }
        return vo;
    }

    private String getDisplayName(Account account) {
        return switch (account.getRole()) {
            case ADMIN -> {
                Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>()
                        .eq(Admin::getAccountId, account.getId()));
                yield admin == null ? account.getUsername() : admin.getRealName();
            }
            case SHIPPER -> {
                Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                        .eq(Shipper::getAccountId, account.getId()));
                yield shipper == null ? account.getUsername() : shipper.getName();
            }
            case LOGISTICS -> {
                Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, account.getId()));
                yield logistics == null ? account.getUsername() : logistics.getName();
            }
            case DRIVER -> {
                Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getAccountId, account.getId()));
                yield driver == null ? account.getUsername() : driver.getName();
            }
        };
    }

    private Realname getLatestRealname(Long accountId) {
        List<Realname> list = realnameMapper.selectList(new LambdaQueryWrapper<Realname>()
                .eq(Realname::getAccountId, accountId)
                .orderByDesc(Realname::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    private Onboarding getLatestOnboarding(Long accountId) {
        List<Onboarding> list = onboardingMapper.selectList(new LambdaQueryWrapper<Onboarding>()
                .eq(Onboarding::getAccountId, accountId)
                .orderByDesc(Onboarding::getId));
        return list.isEmpty() ? null : list.get(0);
    }

    /**
     * 按账号角色和资料邮箱校验找回密码身份。
     *
     * @param username 登录手机号
     * @param email 资料邮箱
     * @param role 账号角色
     * @return 统一账号
     */
    private Account getAccountForPasswordReset(String username, String email, AccountRoleEnum role) {
        Account account = accountMapper.selectOne(new LambdaQueryWrapper<Account>()
                .eq(Account::getUsername, username.trim()));
        if (account == null || account.getRole() != role) {
            throw new BusinessException("账号或邮箱不匹配");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已停用");
        }
        String profileEmail = getProfileEmail(account);
        if (profileEmail == null || !profileEmail.equalsIgnoreCase(email.trim())) {
            throw new BusinessException("账号或邮箱不匹配");
        }
        return account;
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
            case SHIPPER -> {
                Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>()
                        .eq(Shipper::getAccountId, account.getId()));
                yield shipper == null ? null : shipper.getEmail();
            }
            case LOGISTICS -> {
                Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                        .eq(Logistics::getAccountId, account.getId()));
                yield logistics == null ? null : logistics.getEmail();
            }
            case DRIVER -> {
                Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>()
                        .eq(Driver::getAccountId, account.getId()));
                yield driver == null ? null : driver.getEmail();
            }
        };
    }

    /**
     * 清理已过期的重置凭证和验证码，避免长期占用服务内存。
     */
    private void removeExpiredTickets() {
        Instant now = Instant.now();
        passwordResetTickets.entrySet().removeIf(entry -> entry.getValue().expiresAt().isBefore(now));
        passwordResetCodes.entrySet().removeIf(entry -> entry.getValue().getExpiresAt().isBefore(now));
    }

    /**
     * 一次性密码重置凭证记录。
     *
     * @param accountId 账号 ID
     * @param expiresAt 过期时间
     */
    private record PasswordResetTicket(Long accountId, Instant expiresAt) {
    }

    /**
     * 邮箱验证码记录。
     */
    private static final class PasswordResetCodeTicket {

        private final String code;

        private final Instant expiresAt;

        private final Instant resendAt;

        private int failedAttempts;

        private PasswordResetCodeTicket(String code, Instant expiresAt, Instant resendAt) {
            this.code = code;
            this.expiresAt = expiresAt;
            this.resendAt = resendAt;
        }

        public String getCode() {
            return code;
        }

        public Instant getExpiresAt() {
            return expiresAt;
        }

        public Instant getResendAt() {
            return resendAt;
        }

        public int getFailedAttempts() {
            return failedAttempts;
        }

        public void increaseFailedAttempts() {
            failedAttempts++;
        }
    }
}
