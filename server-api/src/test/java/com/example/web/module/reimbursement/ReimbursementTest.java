package com.example.web.module.reimbursement;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 14：报销提交时点、月度额度、跨月归属、特殊审核与驳回重提的 HTTP 接口测试。
 */
class ReimbursementTest extends FreightApiTestBase {

    @Test
    void submitRequiresDepartCheckinAndValidVoucher() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900005001");
        seedApprovedShipper("13900005002");
        String shipperToken = login("13900005002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900005001", DEFAULT_PASSWORD);

        Long firstVehicle = seedVehicle(logisticsAccountId, "京A50001");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A50002");
        Long firstDriverAccountId = seedDriver(logisticsAccountId, firstVehicle, "13900005003");
        Long secondDriverAccountId = seedDriver(logisticsAccountId, secondVehicle, "13900005004");
        String firstDriverToken = login("13900005003", DEFAULT_PASSWORD);
        Long firstDriverId = driverProfileId(firstDriverAccountId);
        Long secondDriverId = driverProfileId(secondDriverAccountId);
        String loadPhoto = seedFile(firstDriverAccountId, "submit-load.jpg");
        String vehiclePhoto = seedFile(firstDriverAccountId, "submit-vehicle.jpg");
        String receipt = seedFile(firstDriverAccountId, "submit-receipt.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, firstTask, firstDriverId, firstVehicle);
        dispatchTask(logisticsToken, secondTask, secondDriverId, secondVehicle);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 100.0, "FUEL", "2026-12-08", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("出发打卡")));

        postJson("/api/driver/tasks/" + firstTask + "/checkin/depart", firstDriverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());
        clock.setNow(LocalDateTime.of(2026, 12, 20, 10, 0));

        data(postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 100.0, "FUEL", "2026-12-08", List.of(receipt))));

        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 0.0, "FUEL", "2026-12-08", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("金额")));
        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, -5.0, "FUEL", "2026-12-08", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("金额")));
        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 80.0, null, "2026-12-08", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("费用类型")));
        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 80.0, "UNKNOWN", "2026-12-08", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("费用类型")));
        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 80.0, "FUEL", "2026-12-08", List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("凭证")));
        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(firstTask, 80.0, "FUEL", "2026-12-25", List.of(receipt)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不能晚于今天")));

        postJson("/api/driver/reimbursements", firstDriverToken,
                reimbursementBody(secondTask, 80.0, "FUEL", "2026-12-08", List.of(receipt)))
                .andExpect(status().isForbidden());
    }

    @Test
    void multipleEntriesOnSameTaskAccumulateDefaultQuota() throws Exception {
        Fixture fixture = prepareDepartedTask("13900005005", "13900005006", "13900005007", "京A50003");
        LocalDateTime submitTime = LocalDateTime.of(2026, 12, 20, 10, 0);
        clock.setNow(submitTime);

        JsonNode first = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 1000.0, "FUEL", "2026-12-08", List.of(fixture.receipt))));
        assertEquals(1, first.path("versionNo").asInt());
        assertEquals("PENDING", first.path("status").asText());
        assertEquals(1000.0, first.path("totalSnapshot").asDouble(), 0.001);
        assertEquals(5000.0, first.path("quotaSnapshot").asDouble(), 0.001);
        assertEquals(0, first.path("overspend").asInt());

        data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 500.0, "TOLL", "2026-12-09", List.of(fixture.receipt))));

        JsonNode page = data(getJson("/api/driver/reimbursements", fixture.driverToken,
                Map.of("pageNum", "1", "pageSize", "10")));
        assertEquals(2, page.path("total").asInt());
        assertEquals("燃油费", page.path("records").get(1).path("expenseTypeName").asText());
        assertTrue(page.path("records").get(1).hasNonNull("taskNo"));
        assertTrue(page.path("records").get(1).hasNonNull("waybillNo"));

        JsonNode quota = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(5000.0, quota.path("quota").asDouble(), 0.001);
        assertEquals(1500.0, quota.path("used").asDouble(), 0.001);
        assertEquals(3500.0, quota.path("remaining").asDouble(), 0.001);
        assertEquals(5000.0, quota.path("defaultQuota").asDouble(), 0.001);
    }

    @Test
    void monthlyTotalOnlyCountsPendingApprovedAndResubmitKeepsHistory() throws Exception {
        Fixture fixture = prepareDepartedTask("13900005008", "13900005009", "13900005010", "京A50004");
        clock.setNow(LocalDateTime.of(2026, 12, 20, 10, 0));

        long reimbursementId = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 1000.0, "FUEL", "2026-12-08", List.of(fixture.receipt))))
                .path("id").asLong();

        postJson("/api/logistics/reimbursements/" + reimbursementId + "/review", fixture.logisticsToken,
                Map.of("action", "REJECT", "note", "凭证不清晰"))
                .andExpect(status().isOk());

        JsonNode rejectedQuota = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(0.0, rejectedQuota.path("used").asDouble(), 0.001);

        JsonNode resubmitted = data(postJson("/api/driver/reimbursements/" + reimbursementId + "/resubmit",
                fixture.driverToken,
                reimbursementBody(fixture.taskId, 1200.0, "TOLL", "2026-12-08", List.of(fixture.receipt))));
        assertEquals(2, resubmitted.path("versionNo").asInt());
        assertEquals("PENDING", resubmitted.path("status").asText());

        JsonNode detail = data(getJson("/api/logistics/reimbursements/" + reimbursementId, fixture.logisticsToken));
        assertEquals(2, detail.path("versions").size());
        assertEquals(1, detail.path("versions").get(0).path("isCurrent").asInt());
        assertEquals(0, detail.path("versions").get(1).path("isCurrent").asInt());
        assertEquals(1, detail.path("reviews").size());
        assertEquals("REJECT", detail.path("reviews").get(0).path("action").asText());

        JsonNode quota = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(1200.0, quota.path("used").asDouble(), 0.001);

        data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 800.0, "FUEL", "2026-11-30", List.of(fixture.receipt))));
        JsonNode november = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-11")));
        assertEquals(800.0, november.path("used").asDouble(), 0.001);
        JsonNode december = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(1200.0, december.path("used").asDouble(), 0.001);
    }

    @Test
    void logisticsQuotaOverrideOnlyAffectsNewSubmissions() throws Exception {
        Fixture fixture = prepareDepartedTask("13900005011", "13900005012", "13900005013", "京A50005");
        clock.setNow(LocalDateTime.of(2026, 12, 20, 10, 0));

        postJson("/api/logistics/driver-quotas", fixture.logisticsToken,
                Map.of("driverAccountId", fixture.driverAccountId, "yearMonth", "2026-12", "amount", 3000.0))
                .andExpect(status().isOk());

        long firstId = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 2000.0, "FUEL", "2026-12-08", List.of(fixture.receipt))))
                .path("id").asLong();
        JsonNode overspend = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 1500.0, "TOLL", "2026-12-08", List.of(fixture.receipt))));
        assertEquals(1, overspend.path("overspend").asInt());
        assertEquals(3500.0, overspend.path("totalSnapshot").asDouble(), 0.001);
        assertEquals(3000.0, overspend.path("quotaSnapshot").asDouble(), 0.001);

        postJson("/api/logistics/driver-quotas", fixture.logisticsToken,
                Map.of("driverAccountId", fixture.driverAccountId, "yearMonth", "2026-12", "amount", 5000.0))
                .andExpect(status().isOk());
        JsonNode firstDetail = data(getJson("/api/logistics/reimbursements/" + firstId, fixture.logisticsToken));
        assertEquals(3000.0, firstDetail.path("versions").get(0).path("quotaSnapshot").asDouble(), 0.001);
        assertEquals(0, firstDetail.path("versions").get(0).path("overspend").asInt());

        JsonNode latest = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 100.0, "FUEL", "2026-12-08", List.of(fixture.receipt))));
        assertEquals(0, latest.path("overspend").asInt());
        assertEquals(5000.0, latest.path("quotaSnapshot").asDouble(), 0.001);
        assertEquals(3600.0, latest.path("totalSnapshot").asDouble(), 0.001);

        JsonNode quota = data(getJson("/api/driver/reimbursements/quota", fixture.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(5000.0, quota.path("quota").asDouble(), 0.001);
        assertEquals(3600.0, quota.path("used").asDouble(), 0.001);
        assertEquals(1400.0, quota.path("remaining").asDouble(), 0.001);
    }

    @Test
    void overspendRequiresSpecialReviewBeforeApproveOrReject() throws Exception {
        Fixture fixture = prepareDepartedTask("13900005014", "13900005015", "13900005016", "京A50006");
        clock.setNow(LocalDateTime.of(2026, 12, 20, 10, 0));

        long normalId = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 50.0, "TOLL", "2026-12-08", List.of(fixture.receipt))))
                .path("id").asLong();
        postJson("/api/logistics/reimbursements/" + normalId + "/review", fixture.logisticsToken,
                Map.of("action", "SPECIAL_REVIEW", "note", "普通单也申请复核"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("仅超限报销单需要特殊复核")));
        postJson("/api/logistics/reimbursements/" + normalId + "/review", fixture.logisticsToken,
                Map.of("action", "REJECT", "note", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("驳回原因")));
        postJson("/api/logistics/reimbursements/" + normalId + "/review", fixture.logisticsToken,
                Map.of("action", "REJECT", "note", "费用不在报销范围"))
                .andExpect(status().isOk());
        JsonNode rejected = data(getJson("/api/driver/reimbursements", fixture.driverToken,
                Map.of("pageNum", "1", "pageSize", "10")));
        assertEquals("费用不在报销范围", rejected.path("records").get(0).path("reviewReason").asText());

        postJson("/api/logistics/driver-quotas", fixture.logisticsToken,
                Map.of("driverAccountId", fixture.driverAccountId, "yearMonth", "2026-12", "amount", 1000.0))
                .andExpect(status().isOk());

        long overspendId = data(postJson("/api/driver/reimbursements", fixture.driverToken,
                reimbursementBody(fixture.taskId, 1200.0, "FUEL", "2026-12-08", List.of(fixture.receipt))))
                .path("id").asLong();

        seedApprovedLogistics("13900005017");
        String otherLogisticsToken = login("13900005017", DEFAULT_PASSWORD);
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", otherLogisticsToken,
                Map.of("action", "APPROVE", "note", "越权审核"))
                .andExpect(status().isForbidden());

        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "APPROVE", "note", "直接通过"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("特殊复核")));
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "REJECT", "note", "直接驳回"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("特殊复核")));
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "SPECIAL_REVIEW", "note", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("复核说明")));
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "SPECIAL_REVIEW", "note", "超限属实，同意报销"))
                .andExpect(status().isOk());

        JsonNode afterReview = data(getJson("/api/logistics/reimbursements/" + overspendId, fixture.logisticsToken));
        assertEquals("超限属实，同意报销", afterReview.path("specialReviewNote").asText());
        assertEquals("PENDING", afterReview.path("auditStatus").asText());

        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "APPROVE", "note", "复核通过"))
                .andExpect(status().isOk());
        JsonNode approved = data(getJson("/api/logistics/reimbursements/" + overspendId, fixture.logisticsToken));
        assertEquals("APPROVED", approved.path("auditStatus").asText());
        assertTrue(approved.hasNonNull("reviewTime"));
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", fixture.logisticsToken,
                Map.of("action", "APPROVE", "note", "重复通过"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("已审核")));

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE notice_type = 'REIMBURSEMENT_APPROVED'",
                Long.class));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE notice_type = 'REIMBURSEMENT_OVERSEND'",
                Long.class));
    }

    private Fixture prepareDepartedTask(String logisticsPhone, String shipperPhone, String driverPhone,
                                        String plateNo) throws Exception {
        Long logisticsAccountId = seedApprovedLogistics(logisticsPhone);
        seedApprovedShipper(shipperPhone);
        String shipperToken = login(shipperPhone, DEFAULT_PASSWORD);
        String logisticsToken = login(logisticsPhone, DEFAULT_PASSWORD);
        Long vehicleId = seedVehicle(logisticsAccountId, plateNo);
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, driverPhone);
        String driverToken = login(driverPhone, DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, plateNo + "-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, plateNo + "-vehicle.jpg");
        String receipt = seedFile(driverAccountId, plateNo + "-receipt.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());
        return new Fixture(logisticsToken, driverAccountId, driverToken, taskId, receipt);
    }

    private Map<String, Object> reimbursementBody(long taskId, Double amount, String expenseType,
                                                  String occurredDate, List<String> photoPaths) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("amount", amount);
        body.put("expenseType", expenseType);
        body.put("occurredDate", occurredDate);
        body.put("description", "现场费用");
        body.put("photoPaths", new ArrayList<>(photoPaths));
        return body;
    }

    private static final class Fixture {

        private final String logisticsToken;
        private final Long driverAccountId;
        private final String driverToken;
        private final long taskId;
        private final String receipt;

        private Fixture(String logisticsToken, Long driverAccountId, String driverToken, long taskId,
                        String receipt) {
            this.logisticsToken = logisticsToken;
            this.driverAccountId = driverAccountId;
            this.driverToken = driverToken;
            this.taskId = taskId;
            this.receipt = receipt;
        }
    }
}
