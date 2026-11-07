package com.example.web.module.account;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.example.web.module.account.entity.Account;
import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 票据 01：多角色账号与手机号登录的 HTTP 接口测试。
 */
class AccountLoginTest extends ApiTestBase {

    @Test
    void adminCanBeCreatedAndLogin() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        String phone = "13900000099";
        JsonNode created = data(postJson("/admin/accounts/admin", adminToken,
                Map.of("phone", phone, "password", "admin888", "realName", "后台管理员", "email", "ops@example.com")));
        assertTrue(created.asLong() > 0);

        JsonNode login = loginBody(phone, "admin888");
        assertEquals("ADMIN", login.path("role").asText());
        assertEquals("admin", login.path("clientType").asText());
        assertFalse(login.path("mustChangePassword").asBoolean());

        postJson("/admin/accounts/admin", adminToken,
                Map.of("phone", phone, "password", "admin888", "realName", "重复管理员", "email", "dup@example.com"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("手机号")));
    }

    @Test
    void registerRejectsDriverAndDuplicatePhone() throws Exception {
        register("SHIPPER", "13900000001", "注册货主", "shipper123");
        register("LOGISTICS", "13900000002", "注册物流", "logistics123");

        loginBody("13900000001", "shipper123").path("role").asText();
        assertEquals("SHIPPER", loginBody("13900000001", "shipper123").path("role").asText());
        assertEquals("LOGISTICS", loginBody("13900000002", "logistics123").path("role").asText());
        assertEquals("web", loginBody("13900000002", "logistics123").path("clientType").asText());

        registerRaw("DRIVER", "13900000003", "自助司机", "driver123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("只允许货主或物流公司")));

        registerRaw("SHIPPER", "13900000001", "重复货主", "shipper123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("手机号已注册")));
    }

    @Test
    void fourRolesCanLoginAndNonAdminCannotAccessAdminApi() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        register("SHIPPER", "13900000001", "货主甲", "shipper123");
        register("LOGISTICS", "13900000002", "物流甲", "logistics123");
        Long logisticsAccountId = accountIdOf("13900000002");
        Long vehicleId = seedVehicle(logisticsAccountId, "京A12345");
        seedDriver(logisticsAccountId, vehicleId, "13700000001");

        String shipperToken = login("13900000001", "shipper123");
        String logisticsToken = login("13900000002", "logistics123");
        String driverToken = login("13700000001", DEFAULT_PASSWORD);

        assertEquals("SHIPPER", loginBody("13900000001", "shipper123").path("role").asText());
        assertEquals("LOGISTICS", loginBody("13900000002", "logistics123").path("role").asText());
        assertEquals("DRIVER", loginBody("13700000001", DEFAULT_PASSWORD).path("role").asText());
        assertEquals("driver", loginBody("13700000001", DEFAULT_PASSWORD).path("clientType").asText());
        assertEquals("admin", loginBody(ADMIN_PHONE, ADMIN_PASSWORD).path("clientType").asText());

        getJson("/admin/accounts", shipperToken).andExpect(status().isForbidden());
        getJson("/admin/accounts", logisticsToken).andExpect(status().isForbidden());
        getJson("/admin/accounts", driverToken).andExpect(status().isForbidden());
        getJson("/admin/admins", shipperToken).andExpect(status().isForbidden());
        data(getJson("/admin/accounts", adminToken, Map.of("pageSize", "100")));
    }

    @Test
    void driverMustChangeInitialPasswordBeforeBusinessAccess() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13600000001");
        Long vehicleId = seedVehicle(logisticsAccountId, "京B00001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13700000099");
        accountMapper.update(null, new LambdaUpdateWrapper<Account>()
                .set(Account::getMustChangePassword, 1)
                .eq(Account::getId, driverAccountId));

        JsonNode login = loginBody("13700000099", DEFAULT_PASSWORD);
        assertTrue(login.path("mustChangePassword").asBoolean());
        String token = login.path("token").asText();

        getJson("/api/realname", token)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("请先修改初始密码")));

        postJson("/api/auth/password", token,
                Map.of("oldPassword", DEFAULT_PASSWORD, "newPassword", "newpass123"))
                .andExpect(status().isOk());

        Account account = accountMapper.selectById(driverAccountId);
        assertEquals(0, account.getMustChangePassword());
        getJson("/api/auth/profile", token).andExpect(status().isUnauthorized());

        JsonNode relogin = loginBody("13700000099", "newpass123");
        assertFalse(relogin.path("mustChangePassword").asBoolean());
        getJson("/api/realname", relogin.path("token").asText()).andExpect(status().isOk());
    }

    @Test
    void disabledAccountCannotLoginAndTokenIsInvalidated() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        register("SHIPPER", "13900000005", "待停用货主", "shipper123");
        Long accountId = accountIdOf("13900000005");
        String token = login("13900000005", "shipper123");

        postJson("/admin/accounts/" + accountId + "/disable", adminToken, Map.of())
                .andExpect(status().isOk());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'ACCOUNT_DISABLE' AND target_id = ?",
                Long.class, accountId));
        getJson("/api/auth/profile", token).andExpect(status().isUnauthorized());
        loginRaw("13900000005", "shipper123")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("停用")));

        postJson("/admin/accounts/" + accountId + "/enable", adminToken, Map.of())
                .andExpect(status().isOk());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'ACCOUNT_ENABLE' AND target_id = ?",
                Long.class, accountId));
        loginBody("13900000005", "shipper123");
    }

    @Test
    void accountListShowsFourRolesWithoutSensitiveFields() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        register("SHIPPER", "13900000001", "测试货主", "shipper123");
        register("LOGISTICS", "13900000002", "测试物流", "logistics123");
        Long logisticsAccountId = accountIdOf("13900000002");
        Long vehicleId = seedVehicle(logisticsAccountId, "京C00001");
        seedDriver(logisticsAccountId, vehicleId, "13700000001");

        JsonNode page = data(getJson("/admin/accounts", adminToken, Map.of("pageSize", "100")));
        assertTrue(page.path("total").asLong() >= 4);
        Set<String> roles = new HashSet<>();
        for (JsonNode record : page.path("records")) {
            roles.add(record.path("role").asText());
            assertTrue(record.hasNonNull("accountId"));
            assertTrue(record.hasNonNull("username"));
            assertTrue(record.hasNonNull("status"));
            assertTrue(record.hasNonNull("displayName"));
            assertTrue(record.hasNonNull("createTime"));
            assertFalse(record.has("idNo"));
            assertFalse(record.has("passwordHash"));
            assertFalse(record.has("passwordSalt"));
        }
        assertTrue(roles.containsAll(Set.of("ADMIN", "SHIPPER", "LOGISTICS", "DRIVER")));

        JsonNode filtered = data(getJson("/admin/accounts", adminToken, Map.of("keyword", "测试物流")));
        assertEquals(1, filtered.path("total").asLong());
        assertEquals("LOGISTICS", filtered.path("records").get(0).path("role").asText());
        assertEquals("测试物流", filtered.path("records").get(0).path("displayName").asText());
    }

    private JsonNode loginBody(String phone, String password) throws Exception {
        byte[] bytes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", phone, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readTree(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).path("data");
    }

    private org.springframework.test.web.servlet.ResultActions loginRaw(String phone, String password) throws Exception {
        return mockMvc.perform(post("/auth/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of("username", phone, "password", password))));
    }

    private void register(String role, String phone, String name, String password) throws Exception {
        registerRaw(role, phone, name, password).andExpect(status().isOk());
    }

    private org.springframework.test.web.servlet.ResultActions registerRaw(String role, String phone, String name,
                                                                           String password) throws Exception {
        return mockMvc.perform(post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(Map.of(
                        "role", role, "phone", phone, "name", name, "email", phone + "@example.com",
                        "password", password))));
    }

    private Long accountIdOf(String phone) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_account WHERE username = ?", Long.class, phone);
    }
}
