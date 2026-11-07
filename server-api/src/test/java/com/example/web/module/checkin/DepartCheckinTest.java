package com.example.web.module.checkin;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 票据 11：出发打卡凭证校验、准时判定、重复提交窗口与一车一单的 HTTP 接口测试。
 */
class DepartCheckinTest extends FreightApiTestBase {

    @Test
    void departRequiresPhotosCoordinatesAndUploaderOwnership() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004001");
        seedApprovedShipper("13900004002");
        String shipperToken = login("13900004002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004001", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A41001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900004003");
        String driverToken = login("13900004003", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "load-done.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "vehicle-state.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of()))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("装货完成")));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆状态")));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(null, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("经度")));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", "/files/checkin/image/missing.jpg"),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("照片文件不存在")));
        String foreignPhoto = seedFile(logisticsAccountId, "foreign.jpg");
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", foreignPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isForbidden());

        JsonNode result = data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto)))));
        assertEquals(1, result.path("versionNo").asInt());
        assertEquals(1, result.path("isValid").asInt());
        assertEquals(1, result.path("onTime").asInt());

        JsonNode checkins = data(getJson("/api/driver/tasks/" + taskId + "/checkins", driverToken));
        assertEquals(1, checkins.path("departVersions").size());
        assertEquals(2, checkins.path("departVersions").get(0).path("photos").size());
    }

    @Test
    void departMarksEarlyAndLateOnTimeWithinSameWindowSemantics() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004004");
        seedApprovedShipper("13900004005");
        String shipperToken = login("13900004005", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004004", DEFAULT_PASSWORD);

        Long firstVehicle = seedVehicle(logisticsAccountId, "京A41002");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A41003");
        Long firstDriverAccountId = seedDriver(logisticsAccountId, firstVehicle, "13900004006");
        Long secondDriverAccountId = seedDriver(logisticsAccountId, secondVehicle, "13900004007");
        String firstDriverToken = login("13900004006", DEFAULT_PASSWORD);
        String secondDriverToken = login("13900004007", DEFAULT_PASSWORD);
        String firstLoad = seedFile(firstDriverAccountId, "early-load.jpg");
        String firstVehiclePhoto = seedFile(firstDriverAccountId, "early-vehicle.jpg");
        String secondLoad = seedFile(secondDriverAccountId, "late-load.jpg");
        String secondVehiclePhoto = seedFile(secondDriverAccountId, "late-vehicle.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long earlyTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long lateTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, earlyTask, driverProfileId(firstDriverAccountId), firstVehicle);
        dispatchTask(logisticsToken, lateTask, driverProfileId(secondDriverAccountId), secondVehicle);

        clock.setNow(LocalDateTime.of(2026, 12, 8, 7, 0));
        postJson("/api/driver/tasks/" + earlyTask + "/checkin/depart", firstDriverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", firstLoad),
                        photo("VEHICLE_STATE", firstVehiclePhoto))))
                .andExpect(status().isOk());
        JsonNode early = data(getJson("/api/driver/tasks/" + earlyTask + "/checkins", firstDriverToken));
        assertEquals(1, early.path("departVersions").get(0).path("onTime").asInt());

        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 25));
        postJson("/api/driver/tasks/" + lateTask + "/checkin/depart", secondDriverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", secondLoad),
                        photo("VEHICLE_STATE", secondVehiclePhoto))))
                .andExpect(status().isOk());
        JsonNode late = data(getJson("/api/driver/tasks/" + lateTask + "/checkins", secondDriverToken));
        assertEquals(0, late.path("departVersions").get(0).path("onTime").asInt());

        assertEquals("IN_PROGRESS", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());
        assertEquals(1, data(getJson("/api/driver/tasks", firstDriverToken, Map.of("tab", "in_transport")))
                .path("total").asInt());
        assertEquals(1, data(getJson("/api/driver/tasks", secondDriverToken, Map.of("tab", "in_transport")))
                .path("total").asInt());
        assertEquals(4L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'TASK_DEPART'",
                Long.class, orderId));
    }

    @Test
    void departRepeatWithinTwentyMinutesCreatesNewVersionAndLocksAfterward() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004008");
        seedApprovedShipper("13900004009");
        String shipperToken = login("13900004009", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004008", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A41004");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900004010");
        String driverToken = login("13900004010", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "repeat-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "repeat-vehicle.jpg");
        String loadPhotoTwo = seedFile(driverAccountId, "repeat-load-2.jpg");
        String vehiclePhotoTwo = seedFile(driverAccountId, "repeat-vehicle-2.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto)))));
        clock.advanceMinutes(19);
        JsonNode secondVersion = data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.5, 39.95, List.of(photo("LOAD_DONE", loadPhotoTwo),
                        photo("VEHICLE_STATE", vehiclePhotoTwo)))));
        assertEquals(2, secondVersion.path("versionNo").asInt());
        assertEquals(1, secondVersion.path("isValid").asInt());

        JsonNode versions = data(getJson("/api/driver/tasks/" + taskId + "/checkins", driverToken))
                .path("departVersions");
        assertEquals(2, versions.size());
        assertEquals(0, versions.get(0).path("isValid").asInt());
        assertEquals(2, versions.get(1).path("versionNo").asInt());
        assertEquals(2, versions.get(1).path("photos").size());

        clock.advanceMinutes(2);
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.6, 39.96, List.of(photo("LOAD_DONE", loadPhotoTwo),
                        photo("VEHICLE_STATE", vehiclePhotoTwo))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("打卡已锁定")));
        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_checkin WHERE task_id = ? AND node = 'DEPART'",
                Long.class, taskId));
        assertEquals("IN_PROGRESS", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());
    }

    @Test
    void departBlockedUntilAllGoodsAreAssigned() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004011");
        seedApprovedShipper("13900004012");
        String shipperToken = login("13900004012", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004011", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A41005");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900004013");
        String driverToken = login("13900004013", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "unassigned-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "unassigned-vehicle.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        Map<String, Object> body = checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                photo("VEHICLE_STATE", vehiclePhoto)));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("全部分配")));
        assertEquals("WAIT_EXECUTE", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, taskId));

        splitTask(logisticsToken, orderId, List.of(goodsIds.get(1)));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken, body)
                .andExpect(status().isOk());
        assertEquals("IN_TRANSPORT", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, taskId));
    }

    @Test
    void sameDriverAndVehicleOnlyOneTaskInTransportUntilCompleted() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004014");
        seedApprovedShipper("13900004015");
        String shipperToken = login("13900004015", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004014", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A41006");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900004016");
        String driverToken = login("13900004016", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "lock-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "lock-vehicle.jpg");
        String unloadPhoto = seedFile(driverAccountId, "lock-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "lock-sign.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, firstTask, driverId, vehicleId);
        dispatchTask(logisticsToken, secondTask, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        Map<String, Object> departBody = checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                photo("VEHICLE_STATE", vehiclePhoto)));
        postJson("/api/driver/tasks/" + firstTask + "/checkin/depart", driverToken, departBody)
                .andExpect(status().isOk());
        postJson("/api/driver/tasks/" + secondTask + "/checkin/depart", driverToken, departBody)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("司机或车辆已有运输中任务")));
        assertEquals(1, data(getJson("/api/driver/tasks", driverToken, Map.of("tab", "in_transport")))
                .path("total").asInt());

        postJson("/api/driver/tasks/" + firstTask + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto))))
                .andExpect(status().isOk());
        assertEquals("COMPLETED", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, firstTask));
        assertEquals(null, jdbcTemplate.queryForObject(
                "SELECT driver_lock FROM biz_task WHERE id = ?", String.class, firstTask));
        assertEquals(null, jdbcTemplate.queryForObject(
                "SELECT vehicle_lock FROM biz_task WHERE id = ?", String.class, firstTask));

        postJson("/api/driver/tasks/" + secondTask + "/checkin/depart", driverToken, departBody)
                .andExpect(status().isOk());
        assertEquals("IN_TRANSPORT", jdbcTemplate.queryForObject(
                "SELECT task_status FROM biz_task WHERE id = ?", String.class, secondTask));
    }

    @Test
    void concurrentDepartSameDriverAndVehicleOnlyOneSucceeds() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900004017");
        seedApprovedShipper("13900004018");
        String shipperToken = login("13900004018", DEFAULT_PASSWORD);
        String logisticsToken = login("13900004017", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A41007");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900004019");
        String driverToken = login("13900004019", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "race-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "race-vehicle.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, firstTask, driverId, vehicleId);
        dispatchTask(logisticsToken, secondTask, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));

        String body = objectMapper.writeValueAsString(checkinBody(116.4, 39.9,
                List.of(photo("LOAD_DONE", loadPhoto), photo("VEHICLE_STATE", vehiclePhoto))));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        String[] results;
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            Callable<String> first = () -> departConcurrently(firstTask, driverToken, body, ready, go);
            Callable<String> second = () -> departConcurrently(secondTask, driverToken, body, ready, go);
            Future<String> firstFuture = executor.submit(first);
            Future<String> secondFuture = executor.submit(second);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            results = new String[]{firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS)};
        } finally {
            executor.shutdownNow();
        }

        int firstStatus = Integer.parseInt(results[0].substring(0, results[0].indexOf('|')));
        int secondStatus = Integer.parseInt(results[1].substring(0, results[1].indexOf('|')));
        assertEquals(1, (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0),
                "同一司机与车辆并发出发必须只有一个任务进入运输中");
        String conflictBody = firstStatus == 400 ? results[0] : results[1];
        assertTrue(conflictBody.contains("司机或车辆已有运输中任务"));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task WHERE order_id = ? AND task_status = 'IN_TRANSPORT'",
                Long.class, orderId));
    }

    private String departConcurrently(long taskId, String token, String body, CountDownLatch ready,
                                      CountDownLatch go) throws Exception {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        MvcResult result = mockMvc.perform(
                        org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                                .post("/api/driver/tasks/" + taskId + "/checkin/depart")
                                .header("Authorization", "Bearer " + token)
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(body))
                .andReturn();
        return result.getResponse().getStatus() + "|"
                + new String(result.getResponse().getContentAsByteArray(), java.nio.charset.StandardCharsets.UTF_8);
    }
}
