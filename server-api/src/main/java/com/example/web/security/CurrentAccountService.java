package com.example.web.security;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.exception.BusinessException;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
import com.example.web.module.driver.entity.Driver;
import com.example.web.module.driver.mapper.DriverMapper;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.module.logistics.mapper.LogisticsMapper;
import com.example.web.module.shipper.entity.Shipper;
import com.example.web.module.shipper.mapper.ShipperMapper;
import org.springframework.stereotype.Service;

/**
 * 当前登录账号与角色资料访问服务，统一校验账号状态、初始密码改密要求与资料归属。
 */
@Service
public class CurrentAccountService {

    private final AccountMapper accountMapper;
    private final AdminMapper adminMapper;
    private final ShipperMapper shipperMapper;
    private final LogisticsMapper logisticsMapper;
    private final DriverMapper driverMapper;

    public CurrentAccountService(AccountMapper accountMapper, AdminMapper adminMapper, ShipperMapper shipperMapper,
                                 LogisticsMapper logisticsMapper, DriverMapper driverMapper) {
        this.accountMapper = accountMapper;
        this.adminMapper = adminMapper;
        this.shipperMapper = shipperMapper;
        this.logisticsMapper = logisticsMapper;
        this.driverMapper = driverMapper;
    }

    /**
     * 获取当前登录账号，并校验账号存在且处于启用状态。
     *
     * @return 当前账号
     */
    public Account getAccount() {
        Long accountId = LoginAccountUtil.getCurrentAccountId();
        Account account = accountMapper.selectById(accountId);
        if (account == null) {
            throw new BusinessException("登录已失效，请重新登录");
        }
        if (account.getStatus() != EnableStatusEnum.ENABLED) {
            throw new BusinessException("账号已停用");
        }
        return account;
    }

    /**
     * 获取当前业务账号，司机未完成首次强制改密时不允许访问业务接口。
     *
     * @return 当前业务账号
     */
    public Account getBusinessAccount() {
        Account account = getAccount();
        if (account.getRole() != AccountRoleEnum.ADMIN && Integer.valueOf(1).equals(account.getMustChangePassword())) {
            throw new BusinessException("请先修改初始密码后再进行业务操作");
        }
        return account;
    }

    /**
     * 获取当前账号 ID。
     *
     * @return 账号 ID
     */
    public Long getAccountId() {
        return getAccount().getId();
    }

    /**
     * 获取当前账号角色。
     *
     * @return 账号角色
     */
    public AccountRoleEnum getRole() {
        return getAccount().getRole();
    }

    /**
     * 获取当前管理员资料。
     *
     * @return 管理员资料
     */
    public Admin getAdmin() {
        Account account = requireRole(AccountRoleEnum.ADMIN);
        Admin admin = adminMapper.selectOne(new LambdaQueryWrapper<Admin>().eq(Admin::getAccountId, account.getId()));
        if (admin == null) {
            throw new BusinessException("管理员资料不存在");
        }
        return admin;
    }

    /**
     * 获取当前货主资料。
     *
     * @return 货主资料
     */
    public Shipper getShipper() {
        Account account = requireBusinessAccountWithRole(AccountRoleEnum.SHIPPER);
        Shipper shipper = shipperMapper.selectOne(new LambdaQueryWrapper<Shipper>().eq(Shipper::getAccountId, account.getId()));
        if (shipper == null) {
            throw new BusinessException("货主资料不存在");
        }
        return shipper;
    }

    /**
     * 获取当前物流公司资料。
     *
     * @return 物流公司资料
     */
    public Logistics getLogistics() {
        Account account = requireBusinessAccountWithRole(AccountRoleEnum.LOGISTICS);
        Logistics logistics = logisticsMapper.selectOne(new LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, account.getId()));
        if (logistics == null) {
            throw new BusinessException("物流公司资料不存在");
        }
        return logistics;
    }

    /**
     * 获取当前司机资料。
     *
     * @return 司机资料
     */
    public Driver getDriver() {
        Account account = requireBusinessAccountWithRole(AccountRoleEnum.DRIVER);
        Driver driver = driverMapper.selectOne(new LambdaQueryWrapper<Driver>().eq(Driver::getAccountId, account.getId()));
        if (driver == null) {
            throw new BusinessException("司机资料不存在");
        }
        return driver;
    }

    /**
     * 校验当前账号角色，不符合时抛出业务异常。
     *
     * @param expected 期望角色
     * @return 当前账号
     */
    public Account requireRole(AccountRoleEnum expected) {
        Account account = getAccount();
        if (account.getRole() != expected) {
            throw new BusinessException("无权访问当前业务");
        }
        return account;
    }

    /**
     * 校验当前账号角色与改密状态。
     *
     * @param expected 期望角色
     * @return 当前账号
     */
    public Account requireBusinessAccountWithRole(AccountRoleEnum expected) {
        Account account = getBusinessAccount();
        if (account.getRole() != expected) {
            throw new BusinessException("无权访问当前业务");
        }
        return account;
    }
}
