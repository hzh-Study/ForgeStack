package com.example.web.module.task;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 10：司机弃单、物流公司强制回收与未出发货物调整的 HTTP 接口测试。
 */
class TaskAbandonReclaimTest extends FreightApiTestBase {

    @Test
    void abandonAndReclaimReturnTaskToPendingAssign() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900003001");
        seedApprovedShipper("13900003002");
        String shipperToken = login("13900003002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900003001", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A31001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900003003");
        String driverToken = login("13900003003", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);

        postJson("/api/driver/tasks/" + taskId + "/abandon", driverToken, Map.of("reason", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("弃单原因不能为空")));
        postJson("/api/driver/tasks/" + taskId + "/abandon", driverToken, Map.of("reason", "车辆临时故障"))
                .andExpect(status().isOk());

        JsonNode abandoned = taskFromWaybill(logisticsToken, orderId, taskId);
        assertEquals("PENDING_ASSIGN", abandoned.path("taskStatus").asText());
        assertTrue(abandoned.path("driverId").isNull());
        assertTrue(abandoned.path("assignType").isNull());
        assertEquals(0, abandoned.path("openClaim").asInt());
        assertEquals(1L, taskEventCount(taskId, "ABANDON"));
        assertEquals(1L, noticeCount(taskId, "TASK_ABANDONED", logisticsAccountId));

        dispatchTask(logisticsToken, taskId, driverId, vehicleId);
        assertEquals("WAIT_EXECUTE", taskFromWaybill(logisticsToken, orderId, taskId)
                .path("taskStatus").asText());

        postJson("/api/logistics/tasks/" + taskId + "/reclaim", logisticsToken, Map.of("reason", ""))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("回收原因不能为空")));
        postJson("/api/logistics/tasks/" + taskId + "/reclaim", logisticsToken, Map.of("reason", "司机临时调离"))
                .andExpect(status().isOk());

        JsonNode reclaimed = taskFromWaybill(logisticsToken, orderId, taskId);
        assertEquals("PENDING_ASSIGN", reclaimed.path("taskStatus").asText());
        assertTrue(reclaimed.path("driverId").isNull());
        assertTrue(reclaimed.path("vehicleId").isNull());
        assertEquals(1L, taskEventCount(taskId, "RECLAIM"));
        assertEquals(1L, noticeCount(taskId, "TASK_RECLAIMED", driverAccountId));

        postJson("/api/logistics/tasks/" + taskId + "/open-claim", logisticsToken, Map.of())
                .andExpect(status().isOk());
        assertEquals(1, data(getJson("/api/driver/task-pool", driverToken, Map.of()))
                .path("total").asLong());
        JsonNode claimed = data(postJson("/api/driver/task-pool/" + taskId + "/claim", driverToken, Map.of()));
        assertEquals("CLAIMED", claimed.path("assignType").asText());
        assertEquals("WAIT_EXECUTE", claimed.path("taskStatus").asText());
    }

    @Test
    void departedTaskCannotBeAbandonedReclaimedOrAdjusted() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900003004");
        seedApprovedShipper("13900003005");
        String shipperToken = login("13900003005", DEFAULT_PASSWORD);
        String logisticsToken = login("13900003004", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A31002");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900003006");
        String driverToken = login("13900003006", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long departedTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long waitingTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, departedTask, driverId, vehicleId);
        dispatchTask(logisticsToken, waitingTask, driverId, vehicleId);
        jdbcTemplate.update("UPDATE biz_task SET task_status = 'IN_TRANSPORT' WHERE id = ?", departedTask);

        postJson("/api/driver/tasks/" + departedTask + "/abandon", driverToken, Map.of("reason", "无法执行"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务已出发")));
        postJson("/api/logistics/tasks/" + departedTask + "/reclaim", logisticsToken,
                Map.of("reason", "司机失联"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务已出发")));
        postJson("/api/logistics/tasks/" + departedTask + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(0), goodsIds.get(1))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务已出发")));
        postJson("/api/logistics/tasks/" + waitingTask + "/reclaim", logisticsToken, Map.of("reason", "  "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("回收原因不能为空")));

        assertEquals(0L, taskEventCount(departedTask, "ABANDON"));
        assertEquals(0L, taskEventCount(departedTask, "RECLAIM"));
    }

    @Test
    void goodsAdjustmentReplacesRowsAndNotifiesAssignedDriver() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900003007");
        seedApprovedShipper("13900003008");
        String shipperToken = login("13900003008", DEFAULT_PASSWORD);
        String logisticsToken = login("13900003007", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A31003");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900003009");
        Long driverId = driverProfileId(driverAccountId);

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);

        postJson("/api/logistics/tasks/" + taskId + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(0), goodsIds.get(1))))
                .andExpect(status().isOk());

        JsonNode adjusted = taskFromWaybill(logisticsToken, orderId, taskId);
        assertEquals(2, adjusted.path("goods").size());
        assertEquals(1L, noticeCount(taskId, "TASK_GOODS_ADJUSTED", driverAccountId));
        assertEquals(1L, taskEventCount(taskId, "GOODS_ADJUST"));

        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(2))).path("taskId").asLong();
        postJson("/api/logistics/tasks/" + taskId + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(2))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物已被其他任务占用")));
        postJson("/api/logistics/tasks/" + secondTask + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物已被其他任务占用")));
        postJson("/api/logistics/tasks/" + secondTask + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(999999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物不属于该订单")));

        postJson("/api/logistics/tasks/" + secondTask + "/goods", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(2))))
                .andExpect(status().isOk());
        assertEquals(0L, noticeCount(secondTask, "TASK_GOODS_ADJUSTED", driverAccountId));
        JsonNode progress = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken)).path("progress");
        assertEquals(3, progress.path("assignedGoods").asInt());
        assertEquals(0, progress.path("unassignedGoods").asInt());
        assertFalse(progress.path("goodsTotal").asInt() < 3);
    }

    private JsonNode taskFromWaybill(String logisticsToken, long orderId, long taskId) throws Exception {
        JsonNode tasks = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken)).path("tasks");
        for (JsonNode task : tasks) {
            if (task.path("taskId").asLong() == taskId) {
                return task;
            }
        }
        throw new IllegalStateException("运单中未找到任务：" + taskId);
    }

    private long taskEventCount(long taskId, String eventType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task_event WHERE task_id = ? AND event_type = ?",
                Long.class, taskId, eventType);
    }

    private long noticeCount(long taskId, String noticeType, Long receiverAccountId) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE task_id = ? AND notice_type = ?"
                        + " AND receiver_account_id = ?", Long.class, taskId, noticeType, receiverAccountId);
    }
}
