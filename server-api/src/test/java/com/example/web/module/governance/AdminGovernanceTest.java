package com.example.web.module.governance;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 16：管理员只读监管、基础资料与参数维护、操作留痕与停用校验的 HTTP 接口测试。
 */
class AdminGovernanceTest extends FreightApiTestBase {

    @Test
    void adminReadsWholeChainDetailWithVersionsPhotosReceiptsAndReimbursementReviews() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        Long logisticsAccountId = seedApprovedLogistics("13900006001");
        Long shipperAccountId = seedApprovedShipper("13900006002");
        String shipperToken = login("13900006002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900006001", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A60001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900006003");
        String driverToken = login("13900006003", DEFAULT_PASSWORD);
        String loadPhoto = seedFile(driverAccountId, "chain-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "chain-vehicle.jpg");
        String unloadPhoto = seedFile(driverAccountId, "chain-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "chain-sign.jpg");
        String receipt = seedFile(driverAccountId, "chain-receipt.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverProfileId(driverAccountId), vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());
        clock.setNow(LocalDateTime.of(2026, 12, 9, 10, 0));
        postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto))))
                .andExpect(status().isOk());
        long reimbursementId = data(postJson("/api/driver/reimbursements", driverToken,
                reimbursementBody(taskId, 300.0, "FUEL", "2026-12-09", List.of(receipt))))
                .path("id").asLong();
        postJson("/api/logistics/reimbursements/" + reimbursementId + "/review", logisticsToken,
                Map.of("action", "APPROVE", "note", "费用合理"))
                .andExpect(status().isOk());

        JsonNode orders = data(getJson("/admin/orders", adminToken,
                Map.of("shipperAccountId", String.valueOf(shipperAccountId), "pageNum", "1", "pageSize", "10")));
        assertEquals(1, orders.path("total").asInt());
        assertTrue(orders.path("records").get(0).hasNonNull("logisticsName"));
        assertTrue(orders.path("records").get(0).hasNonNull("shipperName"));
        JsonNode completed = data(getJson("/admin/orders", adminToken, Map.of("orderStatus", "COMPLETED")));
        assertEquals(1, completed.path("total").asInt());

        JsonNode detail = data(getJson("/admin/orders/" + orderId, adminToken));
        assertEquals(orderId, detail.path("id").asLong());
        assertEquals(1, detail.path("goods").size());
        assertEquals("装货详细地址", detail.path("pickup").path("detail").asText());
        assertEquals("卸货详细地址", detail.path("delivery").path("detail").asText());
        assertEquals("货主13900006002", detail.path("shipperName").asText());
        assertTrue(detail.path("shipperIdNo").asText().length() >= 15);
        assertTrue(detail.path("logisticsName").asText().length() > 0);
        assertEquals(1, detail.path("tasks").size());
        JsonNode task = detail.path("tasks").get(0);
        assertEquals(1, task.path("departCheckins").size());
        assertEquals(1, task.path("arriveCheckins").size());
        assertTrue(task.path("departCheckins").get(0).path("photos").toString().contains("LOAD_DONE"));
        assertTrue(task.path("arriveCheckins").get(0).path("photos").toString().contains("SIGN_RECEIPT"));
        assertEquals(1, task.path("arriveCheckins").get(0).path("onTime").asInt());
        assertEquals(1, detail.path("reimbursements").size());
        assertEquals(1, detail.path("reimbursements").get(0).path("versions").size());
        assertEquals(1, detail.path("reimbursements").get(0).path("reviews").size());
        assertFalse(detail.toString().contains("passwordHash"));

        JsonNode tasks = data(getJson("/admin/tasks", adminToken,
                Map.of("orderId", String.valueOf(orderId), "pageNum", "1", "pageSize", "10")));
        assertEquals(1, tasks.path("total").asInt());
        assertTrue(tasks.path("records").get(0).hasNonNull("driverName"));
        assertTrue(tasks.path("records").get(0).hasNonNull("vehiclePlateNo"));

        JsonNode checkins = data(getJson("/admin/checkins", adminToken,
                Map.of("orderId", String.valueOf(orderId), "node", "DEPART")));
        assertEquals(1, checkins.path("total").asInt());
        assertEquals("LOAD_DONE", checkins.path("records").get(0).path("photos").get(0).path("purpose").asText());
        assertEquals(1, checkins.path("records").get(0).path("onTime").asInt());

        JsonNode reimbursements = data(getJson("/admin/reimbursements", adminToken,
                Map.of("driverAccountId", String.valueOf(driverAccountId), "auditStatus", "APPROVED")));
        assertEquals(1, reimbursements.path("total").asInt());
        JsonNode reimbursementDetail = data(getJson("/admin/reimbursements/" + reimbursementId, adminToken));
        assertEquals("APPROVED", reimbursementDetail.path("auditStatus").asText());
        assertEquals("费用合理", reimbursementDetail.path("reviews").get(0).path("note").asText());

        postJson("/api/messages/conversations", logisticsToken,
                Map.of("targetAccountId", driverAccountId)).andExpect(status().isOk());
        long conversationId = jdbcTemplate.queryForObject(
                "SELECT id FROM biz_conversation ORDER BY id DESC LIMIT 1", Long.class);
        postJson("/api/messages/conversations/" + conversationId + "/send", logisticsToken,
                Map.of("content", "请按时完成签收")).andExpect(status().isOk());
        JsonNode messages = data(getJson("/admin/messages", adminToken, Map.of("pageNum", "1", "pageSize", "10")));
        assertEquals(1, messages.path("total").asInt());

        getJson("/admin/orders", shipperToken).andExpect(status().isForbidden());
        getJson("/admin/orders/" + orderId, driverToken).andExpect(status().isForbidden());
    }

    @Test
    void adminMaintainsDictItemsAndPlatformParamsWithOperationLogs() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        JsonNode expenseItems = data(getJson("/admin/dict-items", adminToken,
                Map.of("dictType", "EXPENSE_TYPE")));
        assertEquals(2, expenseItems.size());

        long createdId = data(postJson("/admin/dict-items", adminToken, dictBody(null, "EXPENSE_TYPE",
                "PARKING", "停车费", null, 3, "ENABLED"))).asLong();
        assertTrue(createdId > 0);
        data(postJson("/admin/dict-items", adminToken, dictBody(createdId, "EXPENSE_TYPE",
                "PARKING", "停车费（调整为其他）", null, 3, "ENABLED")));
        postJson("/admin/dict-items", adminToken, dictBody(null, "EXPENSE_TYPE",
                "FUEL", "重复燃油费", null, 9, "ENABLED"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("已存在")));

        JsonNode filtered = data(getJson("/admin/dict-items", adminToken,
                Map.of("dictType", "EXPENSE_TYPE", "status", "ENABLED")));
        assertEquals(3, filtered.size());
        boolean found = false;
        for (JsonNode item : filtered) {
            if ("PARKING".equals(item.path("itemCode").asText())) {
                found = true;
                assertEquals("停车费（调整为其他）", item.path("itemName").asText());
            }
        }
        assertTrue(found);

        postJson("/admin/platform-params", adminToken, Map.of("paramKey", "reimbursement.monthly.quota",
                "paramValue", "6000", "description", "调整后的月度额度")).andExpect(status().isOk());
        postJson("/admin/platform-params", adminToken, Map.of("paramKey", "order.reminder.points",
                "paramValue", "48,24,1", "description", "到期提醒时点")).andExpect(status().isOk());

        JsonNode params = data(getJson("/admin/platform-params", adminToken));
        boolean quotaUpdated = false;
        boolean reminderCreated = false;
        for (JsonNode param : params) {
            if ("reimbursement.monthly.quota".equals(param.path("paramKey").asText())) {
                quotaUpdated = true;
                assertEquals("6000", param.path("paramValue").asText());
            }
            if ("order.reminder.points".equals(param.path("paramKey").asText())) {
                reminderCreated = true;
                assertEquals("48,24,1", param.path("paramValue").asText());
            }
        }
        assertTrue(quotaUpdated);
        assertTrue(reminderCreated);

        JsonNode dictCreateLogs = data(getJson("/admin/operation-logs", adminToken,
                Map.of("actionType", "DICT_CREATE")));
        assertEquals(1, dictCreateLogs.path("total").asInt());
        JsonNode dictUpdateLogs = data(getJson("/admin/operation-logs", adminToken,
                Map.of("actionType", "DICT_UPDATE")));
        assertEquals(1, dictUpdateLogs.path("total").asInt());
        JsonNode paramLogs = data(getJson("/admin/operation-logs", adminToken,
                Map.of("actionType", "PARAM_UPDATE")));
        assertEquals(2, paramLogs.path("total").asInt());
    }

    @Test
    void disableRejectsOngoingBusinessAndAdminCannotDisableSelf() throws Exception {
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        Long adminAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_account WHERE username = ?", Long.class, ADMIN_PHONE);
        postJson("/admin/accounts/" + adminAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("当前登录")));

        Long logisticsAccountId = seedApprovedLogistics("13900006004");
        seedApprovedShipper("13900006005");
        String shipperToken = login("13900006005", DEFAULT_PASSWORD);
        String logisticsToken = login("13900006004", DEFAULT_PASSWORD);
        Long vehicleId = seedVehicle(logisticsAccountId, "京A60002");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900006006");
        String driverToken = login("13900006006", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverProfileId(driverAccountId), vehicleId);

        postJson("/admin/accounts/" + driverAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("进行中任务")));
        postJson("/admin/accounts/" + logisticsAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("未完成运单")));

        postJson("/api/driver/tasks/" + taskId + "/abandon", driverToken, Map.of("reason", "临时无法执行"))
                .andExpect(status().isOk());
        postJson("/admin/accounts/" + driverAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isOk());
        getJson("/api/driver/tasks", driverToken).andExpect(status().isUnauthorized());

        Long idleLogisticsAccountId = seedApprovedLogistics("13900006007");
        String idleLogisticsToken = login("13900006007", DEFAULT_PASSWORD);
        postJson("/admin/accounts/" + idleLogisticsAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isOk());
        getJson("/api/logistics/waybills", idleLogisticsToken).andExpect(status().isUnauthorized());

        postJson("/api/shipper/orders/" + orderId + "/cancel", shipperToken, Map.of("reason", "运力不足"))
                .andExpect(status().isOk());
        postJson("/admin/accounts/" + logisticsAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isOk());

        Long shipperAccountId = jdbcTemplate.queryForObject(
                "SELECT id FROM sys_account WHERE username = ?", Long.class, "13900006005");
        postJson("/admin/accounts/" + shipperAccountId + "/disable", adminToken, Map.of())
                .andExpect(status().isOk());
    }

    private Map<String, Object> dictBody(Long id, String dictType, String itemCode, String itemName,
                                         String parentCode, Integer sortNo, String status) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("id", id);
        body.put("dictType", dictType);
        body.put("itemCode", itemCode);
        body.put("itemName", itemName);
        body.put("parentCode", parentCode);
        body.put("sortNo", sortNo);
        body.put("status", status);
        return body;
    }

    private Map<String, Object> reimbursementBody(long taskId, Double amount, String expenseType,
                                                  String occurredDate, List<String> photoPaths) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("amount", amount);
        body.put("expenseType", expenseType);
        body.put("occurredDate", occurredDate);
        body.put("description", "现场费用");
        body.put("photoPaths", photoPaths);
        return body;
    }
}
