package com.example.web.module.checkin;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 12：到达打卡、运单完成聚合、打卡版本可见性与追加说明的 HTTP 接口测试。
 */
class ArriveCheckinTest extends FreightApiTestBase {

    @Test
    void arriveRequiresDepartAndPhotosThenCompletesOrder() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900005001");
        seedApprovedShipper("13900005002");
        String shipperToken = login("13900005002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900005001", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A51001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900005003");
        String driverToken = login("13900005003", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "arrive-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "arrive-vehicle.jpg");
        String unloadPhoto = seedFile(driverAccountId, "arrive-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "arrive-sign.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("请先提交出发打卡")));

        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());

        postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("签收凭证")));

        clock.setNow(LocalDateTime.of(2026, 12, 9, 18, 0));
        JsonNode arrive = data(postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto)))));
        assertEquals("ARRIVE", arrive.path("node").asText());
        assertEquals(1, arrive.path("versionNo").asInt());
        assertEquals(1, arrive.path("onTime").asInt());

        assertEquals("COMPLETED", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, taskId));
        assertEquals("COMPLETED", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task_event WHERE task_id = ? AND event_type = 'ARRIVE'",
                Long.class, taskId));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'TASK_ARRIVE'",
                Long.class, orderId));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'WAYBILL_COMPLETED'",
                Long.class, orderId));
    }

    @Test
    void orderAndWaybillDetailsExposeVersionsPhotosSignatureAndRemarks() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900005004");
        seedApprovedShipper("13900005005");
        String shipperToken = login("13900005005", DEFAULT_PASSWORD);
        String logisticsToken = login("13900005004", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A51002");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900005006");
        String driverToken = login("13900005006", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "detail-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "detail-vehicle.jpg");
        String unloadPhoto = seedFile(driverAccountId, "detail-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "detail-sign.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto)))));
        clock.setNow(LocalDateTime.of(2026, 12, 9, 18, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto)))));
        String originalSubmitTime = jdbcTemplate.queryForObject(
                "SELECT submit_time FROM biz_checkin WHERE task_id = ? AND node = 'DEPART'",
                String.class, taskId);
        long originalPhotoCount = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_checkin_photo WHERE task_id = ?", Long.class, taskId);

        JsonNode shipperDetail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertTaskCheckinVisible(shipperDetail.path("tasks").get(0), 1, 1);

        JsonNode waybillDetail = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken));
        assertTaskCheckinVisible(waybillDetail.path("tasks").get(0), 1, 1);
        assertEquals(1, waybillDetail.path("progress").path("arriveCount").asInt());
        assertEquals(1, waybillDetail.path("progress").path("completedCount").asInt());

        postJson("/api/driver/tasks/" + taskId + "/checkin/remark", driverToken,
                Map.of("node", "ARRIVE", "content", "收货方已签字确认"))
                .andExpect(status().isOk());
        postJson("/api/driver/tasks/" + taskId + "/checkin/remark", driverToken,
                Map.of("node", "UNKNOWN", "content", "无效节点"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("打卡节点不正确")));
        postJson("/api/driver/tasks/" + taskId + "/checkin/remark", driverToken,
                Map.of("node", "DEPART", "content", " "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("追加说明不能为空")));

        JsonNode checkins = data(getJson("/api/driver/tasks/" + taskId + "/checkins", driverToken));
        assertEquals(1, checkins.path("departVersions").size());
        assertEquals(1, checkins.path("arriveVersions").size());
        assertEquals(1, checkins.path("remarks").size());
        assertEquals("ARRIVE", checkins.path("remarks").get(0).path("node").asText());
        assertEquals("收货方已签字确认", checkins.path("remarks").get(0).path("content").asText());

        assertEquals(originalSubmitTime, jdbcTemplate.queryForObject(
                "SELECT submit_time FROM biz_checkin WHERE task_id = ? AND node = 'DEPART'",
                String.class, taskId));
        assertEquals(originalPhotoCount, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_checkin_photo WHERE task_id = ?", Long.class, taskId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_checkin WHERE task_id = ? AND node = 'DEPART'",
                Long.class, taskId));
    }

    @Test
    void arriveRepeatWithinWindowKeepsLatestVersionAndAvoidsDuplicateCompletion() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900005007");
        seedApprovedShipper("13900005008");
        String shipperToken = login("13900005008", DEFAULT_PASSWORD);
        String logisticsToken = login("13900005007", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A51003");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900005009");
        String driverToken = login("13900005009", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "repeat-arrive-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "repeat-arrive-vehicle.jpg");
        String unloadPhoto = seedFile(driverAccountId, "repeat-arrive-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "repeat-arrive-sign.jpg");
        String unloadPhotoTwo = seedFile(driverAccountId, "repeat-arrive-unload-2.jpg");
        String signPhotoTwo = seedFile(driverAccountId, "repeat-arrive-sign-2.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto)))));
        clock.setNow(LocalDateTime.of(2026, 12, 9, 18, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto)))));
        assertEquals("COMPLETED", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());

        clock.advanceMinutes(19);
        JsonNode secondVersion = data(postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.31, 23.11, List.of(photo("UNLOAD_DONE", unloadPhotoTwo),
                        photo("SIGN_RECEIPT", signPhotoTwo)))));
        assertEquals(2, secondVersion.path("versionNo").asInt());
        assertEquals(1, secondVersion.path("isValid").asInt());

        JsonNode versions = data(getJson("/api/driver/tasks/" + taskId + "/checkins", driverToken))
                .path("arriveVersions");
        assertEquals(2, versions.size());
        assertEquals(0, versions.get(0).path("isValid").asInt());
        assertEquals(2, versions.get(1).path("versionNo").asInt());
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_checkin WHERE task_id = ? AND node = 'ARRIVE'",
                Long.class, taskId));
        assertEquals("COMPLETED", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, taskId));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'TASK_ARRIVE'",
                Long.class, orderId));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'WAYBILL_COMPLETED'",
                Long.class, orderId));

        clock.advanceMinutes(2);
        postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.32, 23.12, List.of(photo("UNLOAD_DONE", unloadPhotoTwo),
                        photo("SIGN_RECEIPT", signPhotoTwo))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("打卡已锁定")));
    }

    private void assertTaskCheckinVisible(JsonNode task, int departCount, int arriveCount) {
        assertEquals(departCount, task.path("departCheckins").size());
        assertEquals(arriveCount, task.path("arriveCheckins").size());
        JsonNode depart = task.path("departCheckins").get(0);
        assertEquals(1, depart.path("onTime").asInt());
        JsonNode departPhotos = depart.path("photos");
        assertTrue(containsPurpose(departPhotos, "LOAD_DONE"));
        assertTrue(containsPurpose(departPhotos, "VEHICLE_STATE"));
        JsonNode arrive = task.path("arriveCheckins").get(0);
        assertTrue(containsPurpose(arrive.path("photos"), "SIGN_RECEIPT"));
        assertTrue(containsPurpose(arrive.path("photos"), "UNLOAD_DONE"));
        assertEquals(1, arrive.path("onTime").asInt());
        assertNotEquals("", depart.path("submitTime").asText());
        assertFalse(depart.path("isValid").asInt() < 0);
    }

    private boolean containsPurpose(JsonNode photos, String purpose) {
        for (JsonNode photo : photos) {
            if (purpose.equals(photo.path("purpose").asText())) {
                return true;
            }
        }
        return false;
    }
}
