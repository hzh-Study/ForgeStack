package com.example.web.module.driver;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 票据 05：物流公司创建司机、强制改密链路、司机车辆绑定与车辆状态维护的 HTTP 接口测试。
 */
class DriverVehicleManagementTest extends FreightApiTestBase {

    @Test
    void createDriverSupportsInitialPasswordAndRealnameRollback() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004001");
        Long firstVehicle = seedVehicle(logisticsAccountId, "京A40001");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A40002");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900004002");
        Long otherVehicle = seedVehicle(otherLogisticsAccountId, "京B40003");
        String logisticsToken = login("13900004001", DEFAULT_PASSWORD);

        JsonNode created = data(postJson("/api/logistics/drivers", logisticsToken,
                driverBody("新司机甲", "13900004003", "init123", "220101199001014001",
                        "LIC40001", firstVehicle, "2030-12-31")));
        long driverId = created.path("driverId").asLong();
        long driverAccountId = created.path("accountId").asLong();
        assertTrue(driverId > 0);
        assertTrue(driverAccountId > 0);
        assertEquals("ACTIVE", created.path("employmentStatus").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'DRIVER_CREATE' AND target_id = ?",
                Long.class, driverId));

        JsonNode firstLogin = loginBody("13900004003", "init123");
        assertTrue(firstLogin.path("mustChangePassword").asBoolean());
        String initialToken = firstLogin.path("token").asText();
        getJson("/api/driver/vehicle", initialToken)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("请先修改初始密码")));

        postJson("/api/auth/password", initialToken,
                Map.of("oldPassword", "init123", "newPassword", "newpass123"))
                .andExpect(status().isOk());
        getJson("/api/auth/profile", initialToken).andExpect(status().isUnauthorized());

        JsonNode relogin = loginBody("13900004003", "newpass123");
        assertFalse(relogin.path("mustChangePassword").asBoolean());
        JsonNode vehicle = data(getJson("/api/driver/vehicle", relogin.path("token").asText()));
        assertEquals(firstVehicle, vehicle.path("vehicleId").asLong());
        assertEquals("京A40001", vehicle.path("plateNo").asText());

        postJson("/api/logistics/drivers", logisticsToken,
                driverBody("重复手机司机", "13900004003", "init456", "220101199001014002",
                        "LIC40002", secondVehicle, "2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("手机号已注册")));
        postJson("/api/logistics/drivers", logisticsToken,
                driverBody("占用车辆司机", "13900004004", "init456", "220101199001014003",
                        "LIC40003", firstVehicle, "2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆")));
        postJson("/api/logistics/drivers", logisticsToken,
                driverBody("跨公司车辆司机", "13900004005", "init456", "220101199001014004",
                        "LIC40004", otherVehicle, "2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆不属于本公司")));

        postJson("/api/logistics/drivers", logisticsToken,
                driverBody("实名不通过司机", "13900004006", "init456", "220101199001019999",
                        "LIC40005", secondVehicle, "2030-12-31"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("实名")));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM sys_account WHERE username = ?", Long.class, "13900004006"));
        assertEquals(0L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_realname WHERE id_no = ?", Long.class, "220101199001019999"));

        Map<String, Object> account = jdbcTemplate.queryForMap(
                "SELECT role, must_change_password, status FROM sys_account WHERE id = ?", driverAccountId);
        assertEquals("DRIVER", account.get("role"));
        assertEquals(0, ((Number) account.get("must_change_password")).intValue());
        assertEquals("ENABLED", account.get("status"));
        assertEquals(1, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_realname WHERE account_id = ? AND role = 'DRIVER' AND verify_status = 'PASSED'",
                Integer.class, driverAccountId));
        assertEquals(firstVehicle, jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM biz_driver WHERE id = ?", Long.class, driverId));
    }

    @Test
    void driverListOnlyContainsOwnCompanyAndMasksIdNo() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004011");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900004012");
        Long firstVehicle = seedVehicle(logisticsAccountId, "京A40101");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A40102");
        Long otherVehicle = seedVehicle(otherLogisticsAccountId, "京B40103");
        String logisticsToken = login("13900004011", DEFAULT_PASSWORD);

        long firstAccountId = data(postJson("/api/logistics/drivers", logisticsToken,
                driverBody("新司机甲", "13900004013", "init123", "110101199001014101",
                        "LIC40101", firstVehicle, "2031-12-31"))).path("accountId").asLong();
        long secondAccountId = data(postJson("/api/logistics/drivers", logisticsToken,
                driverBody("新司机乙", "13900004014", "init123", "110101199001014102",
                        "LIC40102", secondVehicle, "2031-12-31"))).path("accountId").asLong();
        seedDriver(otherLogisticsAccountId, otherVehicle, "13900004015");

        JsonNode page = data(getJson("/api/logistics/drivers", logisticsToken,
                Map.of("pageNum", "1", "pageSize", "100")));
        assertEquals(2, page.path("total").asLong());
        List<Long> accountIds = new ArrayList<>();
        for (JsonNode record : page.path("records")) {
            accountIds.add(record.path("accountId").asLong());
            assertFalse(record.path("name").asText().isBlank());
            assertFalse(record.path("phone").asText().isBlank());
            assertFalse(record.path("licenseNo").asText().isBlank());
            assertTrue(record.hasNonNull("licenseExpireDate"));
            assertEquals("ACTIVE", record.path("employmentStatus").asText());
            assertTrue(record.path("vehicleId").asLong() > 0);
            assertFalse(record.path("vehiclePlateNo").asText().isBlank());
            assertTrue(record.path("idNo").asText().contains("****"));
            assertFalse(record.has("password"));
            assertFalse(record.has("passwordHash"));
            assertFalse(record.has("passwordSalt"));
        }
        assertTrue(accountIds.contains(firstAccountId));
        assertTrue(accountIds.contains(secondAccountId));
        assertFalse(accountIds.contains(accountIdOf("13900004015")));

        JsonNode filtered = data(getJson("/api/logistics/drivers", logisticsToken,
                Map.of("keyword", "新司机乙", "pageNum", "1", "pageSize", "10")));
        assertEquals(1, filtered.path("total").asLong());
        assertEquals(secondAccountId, filtered.path("records").get(0).path("accountId").asLong());
    }

    @Test
    void bindingAdjustmentChecksVehicleOwnershipAndRunningTasks() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004021");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900004022");
        seedApprovedShipper("13900004023");
        String logisticsToken = login("13900004021", DEFAULT_PASSWORD);
        String shipperToken = login("13900004023", DEFAULT_PASSWORD);

        Long firstVehicle = seedVehicle(logisticsAccountId, "京A40201");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A40202");
        Long thirdVehicle = seedVehicle(logisticsAccountId, "京A40203");
        Long otherVehicle = seedVehicle(otherLogisticsAccountId, "京B40204");
        Long firstDriverAccount = seedDriver(logisticsAccountId, firstVehicle, "13900004024");
        Long secondDriverAccount = seedDriver(logisticsAccountId, secondVehicle, "13900004025");
        Long otherDriverAccount = seedDriver(otherLogisticsAccountId, otherVehicle, "13900004026");
        Long firstDriverId = driverProfileId(firstDriverAccount);
        Long secondDriverId = driverProfileId(secondDriverAccount);
        Long otherDriverId = driverProfileId(otherDriverAccount);

        postJson("/api/logistics/drivers/" + firstDriverId + "/binding", logisticsToken,
                Map.of("vehicleId", secondVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆")));
        postJson("/api/logistics/drivers/" + firstDriverId + "/binding", logisticsToken,
                Map.of("vehicleId", thirdVehicle))
                .andExpect(status().isOk());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_vehicle_binding_log WHERE driver_id = ? AND old_vehicle_id = ?"
                        + " AND new_vehicle_id = ? AND operator_account_id = ?",
                Long.class, firstDriverId, firstVehicle, thirdVehicle, logisticsAccountId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'DRIVER_BINDING_ADJUST'"
                        + " AND target_id = ?", Long.class, firstDriverId));
        assertEquals(thirdVehicle, jdbcTemplate.queryForObject(
                "SELECT vehicle_id FROM biz_driver WHERE id = ?", Long.class, firstDriverId));

        postJson("/api/logistics/drivers/" + otherDriverId + "/binding", logisticsToken,
                Map.of("vehicleId", firstVehicle))
                .andExpect(status().isForbidden());

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long busyTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        splitTask(logisticsToken, orderId, List.of(goodsIds.get(1)));
        dispatchTask(logisticsToken, busyTask, firstDriverId, thirdVehicle);

        postJson("/api/logistics/drivers/" + firstDriverId + "/binding", logisticsToken,
                Map.of("vehicleId", firstVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("进行中任务")))
                .andExpect(jsonPath("$.message", containsString("不能调整绑定")));
        postJson("/api/logistics/drivers/" + secondDriverId + "/binding", logisticsToken,
                Map.of("vehicleId", thirdVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆")));
    }

    @Test
    void vehicleCreateAndStatusMaintenanceEnforceCompanyAndUniqueness() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004031");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900004032");
        String logisticsToken = login("13900004031", DEFAULT_PASSWORD);
        Long otherVehicle = seedVehicle(otherLogisticsAccountId, "京B40301");

        JsonNode created = data(postJson("/api/logistics/vehicles", logisticsToken,
                vehicleBody("京A40301", "HIGH_SIDE", "LIC40301", "2031-12-31",
                        List.of("/files/vehicle/a.jpg", "/files/vehicle/b.png"))));
        long vehicleId = created.path("vehicleId").asLong();
        assertEquals("NORMAL", created.path("vehicleStatus").asText());

        postJson("/api/logistics/vehicles", logisticsToken,
                vehicleBody("京A40301", "HIGH_SIDE", "LIC40302", "2031-12-31", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车牌号已存在")));
        postJson("/api/logistics/vehicles", logisticsToken,
                vehicleBody("京A40302", "UNKNOWN", "LIC40303", "2031-12-31", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆类型")));

        JsonNode page = data(getJson("/api/logistics/vehicles", logisticsToken,
                Map.of("pageNum", "1", "pageSize", "100")));
        assertTrue(page.path("total").asLong() >= 1);
        JsonNode record = findVehicle(page.path("records"), vehicleId);
        assertNotNull(record);
        assertEquals("京A40301", record.path("plateNo").asText());
        assertEquals("HIGH_SIDE", record.path("vehicleType").asText());
        assertEquals("LIC40301", record.path("licenseNo").asText());
        assertEquals("2031-12-31", record.path("licenseExpireDate").asText());
        assertEquals("NORMAL", record.path("vehicleStatus").asText());
        assertEquals("/files/vehicle/a.jpg,/files/vehicle/b.png", record.path("attachmentPaths").asText());
        JsonNode otherCompanyPage = data(getJson("/api/logistics/vehicles",
                login("13900004032", DEFAULT_PASSWORD), Map.of("pageNum", "1", "pageSize", "100")));
        assertTrue(findVehicle(otherCompanyPage.path("records"), vehicleId) == null);

        postJson("/api/logistics/vehicles/" + otherVehicle + "/status", logisticsToken,
                Map.of("status", "REPAIR"))
                .andExpect(status().isForbidden());
        postJson("/api/logistics/vehicles/" + vehicleId + "/status", logisticsToken,
                Map.of("status", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆状态")));
        postJson("/api/logistics/vehicles/" + vehicleId + "/status", logisticsToken,
                Map.of("status", "REPAIR"))
                .andExpect(status().isOk());
        JsonNode repaired = findVehicle(data(getJson("/api/logistics/vehicles", logisticsToken,
                Map.of("pageNum", "1", "pageSize", "100"))).path("records"), vehicleId);
        assertNotNull(repaired);
        assertEquals("REPAIR", repaired.path("vehicleStatus").asText());
        postJson("/api/logistics/vehicles/" + vehicleId + "/status", logisticsToken,
                Map.of("status", "DISABLED"))
                .andExpect(status().isOk());
    }

    @Test
    void unavailableVehicleCannotBeDispatchedOrClaimedAndDriverViewsOwnVehicle() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004041");
        seedApprovedShipper("13900004042");
        String logisticsToken = login("13900004041", DEFAULT_PASSWORD);
        String shipperToken = login("13900004042", DEFAULT_PASSWORD);

        Long repairVehicle = seedVehicle(logisticsAccountId, "京A40401");
        Long normalVehicle = seedVehicle(logisticsAccountId, "京A40402");
        Long repairDriverAccount = seedDriver(logisticsAccountId, repairVehicle, "13900004043");
        Long normalDriverAccount = seedDriver(logisticsAccountId, normalVehicle, "13900004044");
        seedDriver(logisticsAccountId, null, "13900004045");
        Long repairDriverId = driverProfileId(repairDriverAccount);
        Long normalDriverId = driverProfileId(normalDriverAccount);
        String repairDriverToken = login("13900004043", DEFAULT_PASSWORD);
        String normalDriverToken = login("13900004044", DEFAULT_PASSWORD);

        postJson("/api/logistics/vehicles/" + repairVehicle + "/status", logisticsToken,
                Map.of("status", "REPAIR")).andExpect(status().isOk());

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        long thirdTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(2))).path("taskId").asLong();

        postJson("/api/logistics/tasks/" + firstTask + "/dispatch", logisticsToken,
                Map.of("driverId", repairDriverId, "vehicleId", repairVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆当前不可用")));

        postJson("/api/logistics/tasks/" + secondTask + "/open-claim", logisticsToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/driver/task-pool/" + secondTask + "/claim", repairDriverToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆当前不可用")));

        dispatchTask(logisticsToken, thirdTask, normalDriverId, normalVehicle);
        jdbcTemplate.update("UPDATE biz_task SET task_status = 'IN_TRANSPORT' WHERE id = ?", thirdTask);
        postJson("/api/logistics/vehicles/" + normalVehicle + "/status", logisticsToken,
                Map.of("status", "REPAIR"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("进行中任务")));
        postJson("/api/logistics/vehicles/" + normalVehicle + "/status", logisticsToken,
                Map.of("status", "DISABLED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("进行中任务")));

        JsonNode normalVehicleDetail = data(getJson("/api/driver/vehicle", normalDriverToken));
        assertEquals(normalVehicle, normalVehicleDetail.path("vehicleId").asLong());
        assertEquals("京A40402", normalVehicleDetail.path("plateNo").asText());
        assertEquals("NORMAL", normalVehicleDetail.path("vehicleStatus").asText());

        JsonNode repairVehicleDetail = data(getJson("/api/driver/vehicle", repairDriverToken));
        assertEquals(repairVehicle, repairVehicleDetail.path("vehicleId").asLong());
        assertEquals("REPAIR", repairVehicleDetail.path("vehicleStatus").asText());

        JsonNode emptyVehicle = data(getJson("/api/driver/vehicle", login("13900004045", DEFAULT_PASSWORD)));
        assertTrue(emptyVehicle.isNull());
    }

    private Map<String, Object> driverBody(String name, String phone, String password, String idNo,
                                           String licenseNo, Long vehicleId, String licenseExpireDate) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("phone", phone);
        body.put("password", password);
        body.put("idType", "ID_CARD");
        body.put("idNo", idNo);
        body.put("licenseNo", licenseNo);
        body.put("licenseExpireDate", licenseExpireDate);
        body.put("email", phone + "@example.com");
        body.put("vehicleId", vehicleId);
        return body;
    }

    private Map<String, Object> vehicleBody(String plateNo, String vehicleType, String licenseNo,
                                            String licenseExpireDate, List<String> attachmentPaths) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plateNo", plateNo);
        body.put("vehicleType", vehicleType);
        body.put("licenseNo", licenseNo);
        body.put("licenseExpireDate", licenseExpireDate);
        body.put("attachmentPaths", attachmentPaths);
        return body;
    }

    private JsonNode findVehicle(JsonNode records, long vehicleId) {
        for (JsonNode record : records) {
            if (record.path("vehicleId").asLong() == vehicleId) {
                return record;
            }
        }
        return null;
    }

    private Long accountIdOf(String phone) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_account WHERE username = ?", Long.class, phone);
    }

    private JsonNode loginBody(String phone, String password) throws Exception {
        byte[] bytes = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", phone, "password", password))))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsByteArray();
        return objectMapper.readTree(new String(bytes, StandardCharsets.UTF_8)).path("data");
    }
}
