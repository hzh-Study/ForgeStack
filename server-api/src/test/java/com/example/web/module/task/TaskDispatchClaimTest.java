package com.example.web.module.task;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
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
 * 票据 09：任务指派校验、司机知悉确认、任务池隐私与并发领取唯一性的 HTTP 接口测试。
 */
class TaskDispatchClaimTest extends FreightApiTestBase {

    @Test
    void dispatchValidatesCompanyVehicleStatusAndBinding() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900002001");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900002002");
        seedApprovedShipper("13900002003");
        String shipperToken = login("13900002003", DEFAULT_PASSWORD);
        String logisticsToken = login("13900002001", DEFAULT_PASSWORD);

        Long vehicleOne = seedVehicle(logisticsAccountId, "京A21001");
        Long vehicleTwo = seedVehicle(logisticsAccountId, "京A21002");
        Long otherVehicle = seedVehicle(otherLogisticsAccountId, "京B21003");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleOne, "13900002004");
        Long otherDriverAccountId = seedDriver(otherLogisticsAccountId, otherVehicle, "13900002005");
        String driverToken = login("13900002004", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        Long driverId = driverProfileId(driverAccountId);
        Long otherDriverId = driverProfileId(otherDriverAccountId);

        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", otherDriverId, "vehicleId", otherVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("司机不属于本公司")));

        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", otherVehicle))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆不属于本公司")));

        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", vehicleTwo))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆不是该司机绑定的车辆")));

        jdbcTemplate.update("UPDATE biz_vehicle SET vehicle_status = 'REPAIR' WHERE id = ?", vehicleOne);
        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", vehicleOne))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("车辆当前不可用")));
        jdbcTemplate.update("UPDATE biz_vehicle SET vehicle_status = 'NORMAL' WHERE id = ?", vehicleOne);

        dispatchTask(logisticsToken, taskId, driverId, vehicleOne);

        JsonNode detail = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken));
        JsonNode task = detail.path("tasks").get(0);
        assertEquals("WAIT_EXECUTE", task.path("taskStatus").asText());
        assertEquals("ASSIGNED", task.path("assignType").asText());
        assertEquals("司机13900002004", task.path("driverName").asText());
        assertEquals("京A21001", task.path("vehiclePlateNo").asText());

        JsonNode pending = data(getJson("/api/driver/tasks", driverToken, Map.of("tab", "pending")));
        assertEquals(1, pending.path("total").asLong());
        assertEquals("WAIT_EXECUTE", pending.path("records").get(0).path("taskStatus").asText());

        JsonNode driverDetail = data(getJson("/api/driver/tasks/" + taskId, driverToken));
        assertEquals("WAIT_EXECUTE", driverDetail.path("taskStatus").asText());
        assertEquals("ASSIGNED", driverDetail.path("assignType").asText());
        assertEquals(0, driverDetail.path("driverAck").asInt());
        assertEquals("京A21001", driverDetail.path("vehiclePlateNo").asText());
        assertEquals("收货人", driverDetail.path("delivery").path("contactName").asText());
        assertEquals("13700000000", driverDetail.path("delivery").path("contactPhone").asText());
        assertEquals("装货详细地址", driverDetail.path("pickup").path("detail").asText());
        assertFalse(driverDetail.has("freightAmount"));
        assertFalse(driverDetail.path("logisticsName").asText().isEmpty());
        assertFalse(driverDetail.path("shipperName").asText().isEmpty());

        postJson("/api/driver/tasks/" + taskId + "/ack", driverToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/driver/tasks/" + taskId + "/ack", driverToken, Map.of())
                .andExpect(status().isOk());

        JsonNode acked = data(getJson("/api/driver/tasks/" + taskId, driverToken));
        assertEquals(1, acked.path("driverAck").asInt());
        assertTrue(acked.hasNonNull("driverAckTime"));
        assertEquals("ASSIGNED", acked.path("assignType").asText());

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task_event WHERE task_id = ? AND event_type = 'ACK'",
                Long.class, taskId));
        assertEquals(driverAccountId, jdbcTemplate.queryForObject(
                "SELECT driver_account_id FROM biz_task WHERE id = ?", Long.class, taskId));

        postJson("/api/logistics/tasks/" + taskId + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", vehicleOne))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务当前状态不可指派")));
    }

    @Test
    void driverOrVehicleInTransportBlocksDispatchAndClaim() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900002006");
        seedApprovedShipper("13900002007");
        String shipperToken = login("13900002007", DEFAULT_PASSWORD);
        String logisticsToken = login("13900002006", DEFAULT_PASSWORD);

        Long vehicleOne = seedVehicle(logisticsAccountId, "京A21004");
        Long vehicleTwo = seedVehicle(logisticsAccountId, "京A21005");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleOne, "13900002008");
        Long otherDriverAccountId = seedDriver(logisticsAccountId, vehicleTwo, "13900002009");
        String driverToken = login("13900002008", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        Long otherDriverId = driverProfileId(otherDriverAccountId);

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long busyTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long idleTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        long claimTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(2))).path("taskId").asLong();

        dispatchTask(logisticsToken, busyTask, driverId, vehicleOne);
        dispatchTask(logisticsToken, idleTask, otherDriverId, vehicleTwo);
        jdbcTemplate.update("UPDATE biz_task SET task_status = 'IN_TRANSPORT' WHERE id = ?", busyTask);

        postJson("/api/logistics/tasks/" + claimTask + "/dispatch", logisticsToken,
                Map.of("driverId", driverId, "vehicleId", vehicleOne))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("司机或车辆已有运输中任务")));

        postJson("/api/logistics/tasks/" + claimTask + "/open-claim", logisticsToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/driver/task-pool/" + claimTask + "/claim", driverToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("司机或车辆已有运输中任务")));
    }

    @Test
    void taskPoolHidesPrivateInfoAndClaimRejectsOtherCompany() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900002010");
        Long otherLogisticsAccountId = seedApprovedLogistics("13900002011");
        seedApprovedShipper("13900002012");
        String shipperToken = login("13900002012", DEFAULT_PASSWORD);
        String logisticsToken = login("13900002010", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A21006");
        Long otherVehicleId = seedVehicle(otherLogisticsAccountId, "京B21007");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900002013");
        Long otherDriverAccountId = seedDriver(otherLogisticsAccountId, otherVehicleId, "13900002014");
        String driverToken = login("13900002013", DEFAULT_PASSWORD);
        String otherDriverToken = login("13900002014", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();

        postJson("/api/logistics/tasks/" + taskId + "/open-claim", logisticsToken, Map.of())
                .andExpect(status().isOk());

        JsonNode pool = data(getJson("/api/driver/task-pool", driverToken, Map.of()));
        assertEquals(1, pool.path("total").asLong());
        JsonNode poolRecord = pool.path("records").get(0);
        assertEquals(taskId, poolRecord.path("taskId").asLong());
        assertEquals("货物1", poolRecord.path("goodsName").asText());
        assertEquals("北京市", poolRecord.path("pickupProvince").asText());
        String poolJson = pool.toString();
        assertFalse(poolJson.contains("contactName"));
        assertFalse(poolJson.contains("contactPhone"));
        assertFalse(poolJson.contains("detail"));
        assertFalse(poolJson.contains("remark"));

        assertEquals(0, data(getJson("/api/driver/task-pool", otherDriverToken, Map.of()))
                .path("total").asLong());
        postJson("/api/driver/task-pool/" + taskId + "/claim", otherDriverToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务不属于本公司")));

        JsonNode claimed = data(postJson("/api/driver/task-pool/" + taskId + "/claim", driverToken, Map.of()));
        assertEquals("WAIT_EXECUTE", claimed.path("taskStatus").asText());
        assertEquals("CLAIMED", data(getJson("/api/logistics/waybills/" + orderId, logisticsToken))
                .path("tasks").get(0).path("assignType").asText());

        JsonNode driverDetail = data(getJson("/api/driver/tasks/" + taskId, driverToken));
        assertEquals("卸货详细地址", driverDetail.path("delivery").path("detail").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE task_id = ? AND notice_type = 'TASK_CLAIMED'"
                        + " AND receiver_account_id = ?", Long.class, taskId, logisticsAccountId));

        postJson("/api/driver/task-pool/" + taskId + "/claim", driverToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务已被领取")));
        assertEquals(driverAccountId, jdbcTemplate.queryForObject(
                "SELECT driver_account_id FROM biz_task WHERE id = ?", Long.class, taskId));
        assertFalse(otherDriverAccountId.equals(driverAccountId));
    }

    @Test
    void concurrentClaimOnlyOneDriverWins() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900002015");
        seedApprovedShipper("13900002016");
        String shipperToken = login("13900002016", DEFAULT_PASSWORD);
        String logisticsToken = login("13900002015", DEFAULT_PASSWORD);

        Long firstVehicle = seedVehicle(logisticsAccountId, "京A21008");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A21009");
        seedDriver(logisticsAccountId, firstVehicle, "13900002017");
        seedDriver(logisticsAccountId, secondVehicle, "13900002018");
        String firstToken = login("13900002017", DEFAULT_PASSWORD);
        String secondToken = login("13900002018", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long[] taskIds = new long[3];
        for (int index = 0; index < 3; index++) {
            taskIds[index] = splitTask(logisticsToken, orderId, List.of(goodsIds.get(index)))
                    .path("taskId").asLong();
            postJson("/api/logistics/tasks/" + taskIds[index] + "/open-claim", logisticsToken, Map.of())
                    .andExpect(status().isOk());
        }

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            for (int round = 0; round < 3; round++) {
                String path = "/api/driver/task-pool/" + taskIds[round] + "/claim";
                String[] results = raceClaims(executor, path, firstToken, secondToken);
                int firstStatus = Integer.parseInt(results[0].substring(0, results[0].indexOf('|')));
                int secondStatus = Integer.parseInt(results[1].substring(0, results[1].indexOf('|')));
                assertEquals(1, (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0),
                        "并发领取同一任务必须只有一名司机成功");
                String conflictBody = firstStatus == 400 ? results[0] : results[1];
                assertTrue(conflictBody.contains("任务已被领取"));
            }
        } finally {
            executor.shutdownNow();
        }

        int firstDriverCount = data(getJson("/api/driver/tasks", firstToken, Map.of("tab", "pending")))
                .path("total").asInt();
        int secondDriverCount = data(getJson("/api/driver/tasks", secondToken, Map.of("tab", "pending")))
                .path("total").asInt();
        assertEquals(3, firstDriverCount + secondDriverCount);
    }

    /**
     * 让两名司机同时领取同一任务并返回双方结果。
     *
     * @param executor 线程池
     * @param path 领取路径
     * @param firstToken 第一名司机令牌
     * @param secondToken 第二名司机令牌
     * @return 双方状态码与响应体
     */
    private String[] raceClaims(ExecutorService executor, String path, String firstToken, String secondToken)
            throws Exception {
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<String> first = () -> claimConcurrently(path, firstToken, ready, go);
        Callable<String> second = () -> claimConcurrently(path, secondToken, ready, go);
        Future<String> firstFuture = executor.submit(first);
        Future<String> secondFuture = executor.submit(second);
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        go.countDown();
        return new String[]{firstFuture.get(10, TimeUnit.SECONDS), secondFuture.get(10, TimeUnit.SECONDS)};
    }

    /**
     * 并发执行一次任务领取请求。
     *
     * @param path 领取路径
     * @param token 司机令牌
     * @param ready 就绪闩锁
     * @param go 开始闩锁
     * @return 状态码与响应体
     */
    private String claimConcurrently(String path, String token, CountDownLatch ready, CountDownLatch go)
            throws Exception {
        ready.countDown();
        go.await(5, TimeUnit.SECONDS);
        MvcResult result = mockMvc.perform(post(path)
                        .header("Authorization", "Bearer " + token)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andReturn();
        return result.getResponse().getStatus() + "|"
                + new String(result.getResponse().getContentAsByteArray(), StandardCharsets.UTF_8);
    }
}
