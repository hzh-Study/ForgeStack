package com.example.web.module.account.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.page.PageResult;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.dto.AccountQueryDTO;
import com.example.web.module.account.dto.AdminAccountCreateDTO;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.account.service.AccountGovernanceService;
import com.example.web.module.account.vo.AccountListVO;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.governance.enums.OperationActionEnum;
import com.example.web.module.governance.service.OperationLogService;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.order.entity.Order;
import com.example.web.module.order.enums.OrderStatusEnum;
import com.example.web.module.order.mapper.OrderMapper;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.security.LoginAccountUtil;
import com.example.web.util.PasswordUtil;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 管理员账号治理业务实现，按角色资料表组合展示名称并维护账号状态。
 */
@Service
public class AccountGovernanceServiceImpl implements AccountGovernanceService {

    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;
    private final ShipperMapper shipperMapper;
    private final LogisticsMapper logisticsMapper;
    private final DriverMapper driverMapper;
    private final TaskMapper taskMapper;
    private final OrderMapper orderMapper;
    private final OperationLogService operationLogService;

    public AccountGovernanceServiceImpl(AccountMapper accountMapper, AdminMapper adminMapper,
                                        ShipperMapper shipperMapper, LogisticsMapper logisticsMapper,
                                        DriverMapper driverMapper, TaskMapper taskMapper, OrderMapper orderMapper,
                                        OperationLogService operationLogService) {
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
        this.shipperMapper = shipperMapper;
        this.logisticsMapper = logisticsMapper;
        this.driverMapper = driverMapper;
        this.taskMapper = taskMapper;
        this.orderMapper = orderMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * 事务创建管理员账号与管理员资料。
     *
     * @param dto 创建参数
     * @return 新建账号 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public Long createAdminAccount(AdminAccountCreateDTO dto) {
        String phone = dto.getPhone().trim();
        if (accountMapper.selectCount(new LambdaQueryWrapper<Account>().eq(Account::getUsername, phone)) > 0) {
            throw new BusinessException("该手机号已被其他账号使用");
        }
        String salt = PasswordUtil.generateSalt();
        Account account = new Account();
        account.setRole(AccountRoleEnum.ADMIN);
        account.setUsername(phone);
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(dto.getPassword(), salt));
        account.setStatus(EnableStatusEnum.ENABLED);
        account.setMustChangePassword(0);
        accountMapper.insert(account);

        Admin admin = new Admin();
        admin.setAccountId(account.getId());
        admin.setRealName(dto.getRealName().trim());
        admin.setPhone(phone);
        admin.setEmail(dto.getEmail());
        adminMapper.insert(admin);
        return account.getId();
    }

    /**
     * 分页查询四类账号，展示名称取自各自角色资料表。
     *
     * @param dto 查询参数
     * @return 账号分页数据
     */
    @Override
    public PageResult<AccountListVO> pageAccounts(AccountQueryDTO dto) {
        Page<Account> page = accountMapper.selectPage(new Page<>(dto.getPageNum(), dto.getPageSize()),
                buildPageWrapper(dto));
        Map<Long, String> displayNames = loadDisplayNames(page.getRecords());
        List<AccountListVO> records = page.getRecords().stream()
                .map(account -> toListVO(account, displayNames.get(account.getId())))
                .toList();
        return new PageResult<>(records, page.getTotal(), dto.getPageNum(), dto.getPageSize());
    }

    /**
     * 停用账号、清理登录态并记录操作留痕。
     *
     * @param accountId 账号 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void disableAccount(Long accountId) {
        Account account = getExistingAccount(accountId);
        Long operatorAccountId = LoginAccountUtil.getCurrentAccountId();
        if (accountId.equals(operatorAccountId)) {
            throw new BusinessException("不能停用当前登录账号");
        }
        if (account.getStatus() == EnableStatusEnum.DISABLED) {
            return;
        }
        validateNoOngoingBusiness(account);
        updateStatus(accountId, EnableStatusEnum.DISABLED);
        LoginAccountUtil.logout(accountId);
        operationLogService.record(operatorAccountId, AccountRoleEnum.ADMIN, OperationActionEnum.ACCOUNT_DISABLE,
                "ACCOUNT", accountId, "停用账号：" + account.getUsername());
    }

    /**
     * 启用账号并记录操作留痕。
     *
     * @param accountId 账号 ID
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void enableAccount(Long accountId) {
        Account account = getExistingAccount(accountId);
        if (account.getStatus() == EnableStatusEnum.ENABLED) {
            return;
        }
        updateStatus(accountId, EnableStatusEnum.ENABLED);
        operationLogService.record(LoginAccountUtil.getCurrentAccountId(), AccountRoleEnum.ADMIN,
                OperationActionEnum.ACCOUNT_ENABLE, "ACCOUNT", accountId, "启用账号：" + account.getUsername());
    }

    private Account getExistingAccount(Long accountId) {
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException("账号不存在");
        }
        return account;
    }

    /**
     * 停用前校验账号是否仍有进行中业务，避免产生无人执行的任务或未完成运单。
     *
     * @param account 待停用账号
     */
    private void validateNoOngoingBusiness(Account account) {
        if (account.getRole() == AccountRoleEnum.DRIVER) {
            long count = taskMapper.selectCount(new LambdaQueryWrapper<Task>()
                    .eq(Task::getDriverAccountId, account.getId())
                    .in(Task::getTaskStatus, Set.of(TaskStatusEnum.WAIT_EXECUTE.getCode(),
                            TaskStatusEnum.IN_TRANSPORT.getCode())));
            if (count > 0) {
                throw new BusinessException("司机有进行中任务，不能停用");
            }
            return;
        }
        if (account.getRole() == AccountRoleEnum.LOGISTICS) {
            long count = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                    .eq(Order::getLogisticsAccountId, account.getId())
                    .in(Order::getOrderStatus, Set.of(OrderStatusEnum.ACCEPTED.getCode(),
                            OrderStatusEnum.DISPATCHING.getCode(), OrderStatusEnum.IN_PROGRESS.getCode())));
            if (count > 0) {
                throw new BusinessException("物流公司有未完成运单，不能停用");
            }
        }
    }

    private void updateStatus(Long accountId, EnableStatusEnum status) {
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getStatus, status)
                .eq(Account::getId, accountId));
    }

    private LambdaQueryWrapper<Account> buildPageWrapper(AccountQueryDTO dto) {
        LambdaQueryWrapper<Account> wrapper = new LambdaQueryWrapper<>();
        if (dto.getRole() != null) {
            wrapper.eq(Account::getRole, dto.getRole());
        }
        if (dto.getStatus() != null) {
            wrapper.eq(Account::getStatus, dto.getStatus());
        }
        String keyword = dto.getKeyword() == null ? "" : dto.getKeyword().trim();
        if (!keyword.isEmpty()) {
            List<Long> matchedAccountIds = findAccountIdsByDisplayName(keyword);
            wrapper.and(item -> {
                item.like(Account::getUsername, keyword);
                if (!matchedAccountIds.isEmpty()) {
                    item.or().in(Account::getId, matchedAccountIds);
                }
            });
        }
        return wrapper.orderByDesc(Account::getId);
    }

    /**
     * 按角色资料表匹配展示名称，返回命中的账号 ID 列表。
     *
     * @param keyword 关键词
     * @return 命中的账号 ID
     */
    private List<Long> findAccountIdsByDisplayName(String keyword) {
        List<Long> accountIds = new ArrayList<>();
        adminMapper.selectList(new LambdaQueryWrapper<Admin>().like(Admin::getRealName, keyword))
                .forEach(admin -> accountIds.add(admin.getAccountId()));
        shipperMapper.selectList(new LambdaQueryWrapper<Shipper>().like(Shipper::getName, keyword))
                .forEach(shipper -> accountIds.add(shipper.getAccountId()));
        logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>().like(Logistics::getName, keyword))
                .forEach(logistics -> accountIds.add(logistics.getAccountId()));
        driverMapper.selectList(new LambdaQueryWrapper<Driver>().like(Driver::getName, keyword))
                .forEach(driver -> accountIds.add(driver.getAccountId()));
        return accountIds;
    }

    /**
     * 按角色批量加载展示名称，避免逐条查询。
     *
     * @param accounts 当前页账号
     * @return 账号 ID 与展示名称映射
     */
    private Map<Long, String> loadDisplayNames(List<Account> accounts) {
        Map<Long, String> displayNames = new HashMap<>();
        List<Long> adminIds = new ArrayList<>();
        List<Long> shipperIds = new ArrayList<>();
        List<Long> logisticsIds = new ArrayList<>();
        List<Long> driverIds = new ArrayList<>();
        for (Account account : accounts) {
            switch (account.getRole()) {
                case ADMIN -> adminIds.add(account.getId());
                case SHIPPER -> shipperIds.add(account.getId());
                case LOGISTICS -> logisticsIds.add(account.getId());
                case DRIVER -> driverIds.add(account.getId());
            }
        }
        if (!adminIds.isEmpty()) {
            adminMapper.selectList(new LambdaQueryWrapper<Admin>().in(Admin::getAccountId, adminIds))
                    .forEach(admin -> displayNames.put(admin.getAccountId(), admin.getRealName()));
        }
        if (!shipperIds.isEmpty()) {
            shipperMapper.selectList(new LambdaQueryWrapper<Shipper>().in(Shipper::getAccountId, shipperIds))
                    .forEach(shipper -> displayNames.put(shipper.getAccountId(), shipper.getName()));
        }
        if (!logisticsIds.isEmpty()) {
            logisticsMapper.selectList(new LambdaQueryWrapper<Logistics>().in(Logistics::getAccountId, logisticsIds))
                    .forEach(logistics -> displayNames.put(logistics.getAccountId(), logistics.getName()));
        }
        if (!driverIds.isEmpty()) {
            driverMapper.selectList(new LambdaQueryWrapper<Driver>().in(Driver::getAccountId, driverIds))
                    .forEach(driver -> displayNames.put(driver.getAccountId(), driver.getName()));
        }
        return displayNames;
    }

    private AccountListVO toListVO(Account account, String displayName) {
        AccountListVO vo = new AccountListVO();
        vo.setAccountId(account.getId());
        vo.setRole(account.getRole().getCode());
        vo.setRoleName(account.getRole().getName());
        vo.setUsername(account.getUsername());
        vo.setStatus(account.getStatus());
        vo.setDisplayName(displayName == null ? account.getUsername() : displayName);
        vo.setCreateTime(account.getCreateTime());
        return vo;
    }
}
