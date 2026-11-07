package com.example.web.support;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.common.enums.EnableStatusEnum;
import com.example.web.common.time.ClockService;
import com.example.web.module.account.entity.Account;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.account.mapper.AccountMapper;
import com.example.web.module.admin.entity.Admin;
import com.example.web.module.admin.mapper.AdminMapper;
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
import com.example.web.util.PasswordUtil;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * HTTP API 测试基类，只通过真实接口验证业务行为，并统一准备账号与资质种子数据。
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Import(TestClockConfig.class)
public abstract class ApiTestBase {

    protected static final String ADMIN_PHONE = "13800000000";
    protected static final String ADMIN_PASSWORD = "admin123";
    protected static final String DEFAULT_PASSWORD = "123456";

    @Autowired
    protected MockMvc mockMvc;

    @Autowired
    protected ObjectMapper objectMapper;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @Autowired
    protected MutableClockService clock;

    @Autowired
    protected AccountMapper accountMapper;

    @Autowired
    protected AdminMapper adminMapper;

    @Autowired
    protected ShipperMapper shipperMapper;

    @Autowired
    protected LogisticsMapper logisticsMapper;

    @Autowired
    protected DriverMapper driverMapper;

    @Autowired
    protected RealnameMapper realnameMapper;

    @Autowired
    protected OnboardingMapper onboardingMapper;

    @Autowired
    protected VehicleMapper vehicleMapper;

    private static final List<String> TABLES = List.of(
            "sys_file", "sys_platform_param", "sys_dict_item", "sys_account", "sys_admin",
            "biz_shipper", "biz_logistics", "biz_realname", "biz_onboarding", "biz_onboarding_file",
            "biz_vehicle", "biz_driver", "biz_vehicle_binding_log",
            "biz_order", "biz_order_goods", "biz_order_address",
            "biz_task", "biz_task_goods", "biz_task_event",
            "biz_checkin", "biz_checkin_photo", "biz_checkin_remark",
            "biz_reimbursement", "biz_reimbursement_photo", "biz_reimbursement_review", "biz_driver_quota",
            "biz_notification", "biz_conversation", "biz_conversation_message",
            "biz_operation_log", "biz_reminder_record");

    /**
     * 每个用例开始前清空业务数据并准备平台参数、字典与管理员账号。
     */
    @BeforeEach
    void resetDatabase() {
        clock.reset();
        for (String table : TABLES) {
            jdbcTemplate.update("DELETE FROM " + table);
        }
        jdbcTemplate.update("INSERT INTO sys_platform_param (param_key, param_value, description) VALUES "
                + "('order.default.valid.hours','240','订单默认有效期'),"
                + "('reimbursement.monthly.quota','5000','月度额度默认值'),"
                + "('checkin.window.minutes','20','打卡窗口'),"
                + "('order.reminder.hours.2d','48','到期前 2 天提醒'),"
                + "('order.reminder.hours.1d','24','到期前 1 天提醒'),"
                + "('order.reminder.hours.1h','1','到期前 1 小时提醒')");
        jdbcTemplate.update("INSERT INTO sys_dict_item (dict_type, item_code, item_name, sort_no, status) VALUES "
                + "('GOODS_TYPE','COAL','煤炭',1,'ENABLED'),"
                + "('GOODS_TYPE','STEEL','钢材',2,'ENABLED'),"
                + "('GOODS_TYPE','DAILY','日用品',3,'ENABLED'),"
                + "('EXPENSE_TYPE','FUEL','燃油费',1,'ENABLED'),"
                + "('EXPENSE_TYPE','TOLL','过路费',2,'ENABLED'),"
                + "('VEHICLE_TYPE','HIGH_SIDE','高栏车',1,'ENABLED'),"
                + "('REGION','110000','北京市',1,'ENABLED')");
        seedAdmin();
    }

    protected Long seedAdmin() {
        Account account = createAccount(AccountRoleEnum.ADMIN, ADMIN_PHONE, ADMIN_PASSWORD);
        Admin admin = new Admin();
        admin.setAccountId(account.getId());
        admin.setRealName("平台管理员");
        admin.setPhone(ADMIN_PHONE);
        admin.setEmail("admin@example.com");
        adminMapper.insert(admin);
        return account.getId();
    }

    protected Account createAccount(AccountRoleEnum role, String phone, String password) {
        Account account = new Account();
        account.setRole(role);
        account.setUsername(phone);
        String salt = PasswordUtil.generateSalt();
        account.setPasswordSalt(salt);
        account.setPasswordHash(PasswordUtil.hashPassword(password, salt));
        account.setStatus(EnableStatusEnum.ENABLED);
        account.setMustChangePassword(0);
        accountMapper.insert(account);
        return account;
    }

    /**
     * 创建已实名的货主账号。
     *
     * @param phone 手机号
     * @return 账号 ID
     */
    protected Long seedShipper(String phone) {
        Account account = createAccount(AccountRoleEnum.SHIPPER, phone, DEFAULT_PASSWORD);
        Shipper shipper = new Shipper();
        shipper.setAccountId(account.getId());
        shipper.setName("货主" + phone);
        shipper.setPhone(phone);
        shipperMapper.insert(shipper);
        insertRealname(account.getId(), AccountRoleEnum.SHIPPER, phone, "PASSED", null, null);
        return account.getId();
    }

    /**
     * 创建已入驻通过的货主账号。
     *
     * @param phone 手机号
     * @return 账号 ID
     */
    protected Long seedApprovedShipper(String phone) {
        Long accountId = seedShipper(phone);
        insertOnboarding(accountId, AccountRoleEnum.SHIPPER, phone, "APPROVED", null);
        return accountId;
    }

    /**
     * 创建已实名并入驻通过的物流公司账号。
     *
     * @param phone 手机号
     * @return 账号 ID
     */
    protected Long seedApprovedLogistics(String phone) {
        Account account = createAccount(AccountRoleEnum.LOGISTICS, phone, DEFAULT_PASSWORD);
        Logistics logistics = new Logistics();
        logistics.setAccountId(account.getId());
        logistics.setName("物流" + phone);
        logistics.setPhone(phone);
        logisticsMapper.insert(logistics);
        insertRealname(account.getId(), AccountRoleEnum.LOGISTICS, phone, "PASSED", null, null);
        insertOnboarding(account.getId(), AccountRoleEnum.LOGISTICS, phone, "APPROVED", null);
        return account.getId();
    }

    /**
     * 创建物流公司车辆。
     *
     * @param logisticsAccountId 物流公司账号 ID
     * @param plateNo 车牌号
     * @return 车辆 ID
     */
    protected Long seedVehicle(Long logisticsAccountId, String plateNo) {
        Logistics logistics = logisticsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, logisticsAccountId));
        Vehicle vehicle = new Vehicle();
        vehicle.setLogisticsId(logistics.getId());
        vehicle.setPlateNo(plateNo);
        vehicle.setVehicleType("HIGH_SIDE");
        vehicle.setLicenseNo("LIC" + plateNo);
        vehicle.setLicenseExpireDate(LocalDate.now().plusYears(5));
        vehicle.setVehicleStatus("NORMAL");
        vehicleMapper.insert(vehicle);
        return vehicle.getId();
    }

    /**
     * 创建司机账号并绑定公司车辆。
     *
     * @param logisticsAccountId 物流公司账号 ID
     * @param vehicleId 车辆 ID
     * @param phone 司机手机号
     * @return 司机账号 ID
     */
    protected Long seedDriver(Long logisticsAccountId, Long vehicleId, String phone) {
        Account account = createAccount(AccountRoleEnum.DRIVER, phone, DEFAULT_PASSWORD);
        Logistics logistics = logisticsMapper.selectOne(new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<Logistics>()
                .eq(Logistics::getAccountId, logisticsAccountId));
        Realname realname = insertRealname(account.getId(), AccountRoleEnum.DRIVER, phone, "PASSED", null, null);
        Driver driver = new Driver();
        driver.setAccountId(account.getId());
        driver.setLogisticsId(logistics.getId());
        driver.setRealnameId(realname.getId());
        driver.setVehicleId(vehicleId);
        driver.setName("司机" + phone);
        driver.setPhone(phone);
        driver.setLicenseNo("DRV" + phone);
        driver.setLicenseExpireDate(LocalDate.now().plusYears(5));
        driver.setEmploymentStatus("ACTIVE");
        driverMapper.insert(driver);
        return account.getId();
    }

    protected Realname insertRealname(Long accountId, AccountRoleEnum role, String phone, String status,
                                       String failReason, Long enterpriseOnboardingId) {
        Realname realname = new Realname();
        realname.setAccountId(accountId);
        realname.setRole(role.getCode());
        realname.setRealnameType("PERSONAL");
        realname.setRealName("实名" + phone);
        realname.setIdType("ID_CARD");
        realname.setIdNo(buildIdNo(phone));
        realname.setPhone(phone);
        realname.setVerifyStatus(status);
        realname.setFailReason(failReason);
        realname.setEnterpriseOnboardingId(enterpriseOnboardingId);
        realname.setVerifyTime(LocalDateTime.now());
        realnameMapper.insert(realname);
        return realname;
    }

    protected Onboarding insertOnboarding(Long accountId, AccountRoleEnum role, String phone, String status,
                                          String rejectReason) {
        Onboarding onboarding = new Onboarding();
        onboarding.setAccountId(accountId);
        onboarding.setRole(role.getCode());
        onboarding.setSubjectName("主体" + phone);
        onboarding.setCreditCode("91" + buildIdNo(phone));
        onboarding.setCreditVerifyStatus("PASSED");
        onboarding.setLegalRepName("法人" + phone);
        onboarding.setLegalRepIdType("ID_CARD");
        onboarding.setLegalRepIdNo("99" + buildIdNo(phone));
        onboarding.setContactName("联系人" + phone);
        onboarding.setContactPhone(phone);
        onboarding.setProvince("北京市");
        onboarding.setCity("北京市");
        onboarding.setDistrict("朝阳区");
        onboarding.setBusinessScope("普通货运");
        onboarding.setAuditStatus(status);
        onboarding.setRejectReason(rejectReason);
        onboardingMapper.insert(onboarding);
        return onboarding;
    }

    private String buildIdNo(String phone) {
        String digits = phone.replaceAll("[^0-9]", "");
        return "11010119900101" + digits.substring(Math.max(0, digits.length() - 4));
    }

    /**
     * 登录并返回 Token，未成功时直接断言失败。
     *
     * @param phone 手机号
     * @param password 密码
     * @return Token
     */
    protected String login(String phone, String password) {
        try {
            String response = mockMvc.perform(post("/auth/login")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(Map.of("username", phone, "password", password))))
                    .andExpect(status().isOk())
                    .andReturn().getResponse().getContentAsString();
            JsonNode node = objectMapper.readTree(response);
            return node.path("data").path("token").asText();
        } catch (Exception exception) {
            throw new IllegalStateException("测试账号登录失败：" + phone, exception);
        }
    }

    protected ResultActions getJson(String path, String token) throws Exception {
        return mockMvc.perform(get(path).header("Authorization", "Bearer " + token));
    }

    protected ResultActions getJson(String path, String token, Map<String, String> params) throws Exception {
        var builder = get(path).header("Authorization", "Bearer " + token);
        for (Map.Entry<String, String> entry : params.entrySet()) {
            builder.param(entry.getKey(), entry.getValue());
        }
        return mockMvc.perform(builder);
    }

    protected ResultActions postJson(String path, String token, Object body) throws Exception {
        return mockMvc.perform(post(path)
                .header("Authorization", "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(body)));
    }

    protected JsonNode data(ResultActions actions) throws Exception {
        byte[] bytes = actions.andExpect(status().isOk()).andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readTree(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).path("data");
    }

    protected ClockService clockService() {
        return clock;
    }
}
