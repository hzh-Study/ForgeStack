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
 * 票据 08：运单列表、整行拆分、增量拆分与货物占用约束的 HTTP 接口测试。
 */
class WaybillSplitTest extends FreightApiTestBase {

    @Test
    void waybillSupportsWholeRowSplitAndIncrementalSplit() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900001001");
        seedApprovedShipper("13900001002");
        String shipperToken = login("13900001002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900001001", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 3);
        acceptOrder(logisticsToken, orderId);

        JsonNode list = data(getJson("/api/logistics/waybills", logisticsToken, Map.of("pageSize", "10")));
        assertEquals(1, list.path("total").asLong());
        JsonNode summary = list.path("records").get(0);
        assertEquals(3, summary.path("goodsCount").asInt());
        assertEquals(0, summary.path("assignedGoodsCount").asInt());
        assertEquals(0, summary.path("taskCount").asInt());
        assertFalse(summary.path("orderNo").asText().isEmpty());
        assertFalse(summary.path("waybillNo").asText().isEmpty());
        assertTrue(summary.hasNonNull("acceptTime"));

        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        assertEquals(3, goodsIds.size());

        JsonNode firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0)));
        assertEquals("PENDING_ASSIGN", firstTask.path("taskStatus").asText());
        assertFalse(firstTask.path("taskNo").asText().isEmpty());

        JsonNode detail = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken));
        assertEquals("DISPATCHING", detail.path("orderStatus").asText());
        JsonNode firstGoods = detail.path("goods").get(0);
        assertTrue(firstGoods.path("assigned").asBoolean());
        assertEquals(firstTask.path("taskId").asLong(), firstGoods.path("taskId").asLong());
        assertEquals(firstTask.path("taskNo").asText(), firstGoods.path("taskNo").asText());
        assertFalse(detail.path("goods").get(1).path("assigned").asBoolean());
        assertEquals(1, detail.path("tasks").size());
        assertEquals(3, detail.path("progress").path("goodsTotal").asInt());
        assertEquals(1, detail.path("progress").path("assignedGoods").asInt());

        JsonNode secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1), goodsIds.get(2)));
        assertEquals("PENDING_ASSIGN", secondTask.path("taskStatus").asText());

        JsonNode afterSplit = data(getJson("/api/logistics/waybills/" + orderId, logisticsToken));
        assertEquals(2, afterSplit.path("tasks").size());
        assertEquals(3, afterSplit.path("progress").path("assignedGoods").asInt());
        assertEquals(0, afterSplit.path("progress").path("unassignedGoods").asInt());
        assertEquals("张三", afterSplit.path("pickup").path("contactName").asText());
        assertEquals("装货详细地址", afterSplit.path("pickup").path("detail").asText());
        assertEquals("收货人", afterSplit.path("delivery").path("contactName").asText());
        assertEquals(2, afterSplit.path("tasks").get(1).path("goods").size());

        JsonNode listAfter = data(getJson("/api/logistics/waybills", logisticsToken, Map.of("pageSize", "10")));
        assertEquals(3, listAfter.path("records").get(0).path("assignedGoodsCount").asInt());
        assertEquals(2, listAfter.path("records").get(0).path("taskCount").asInt());
    }

    @Test
    void sameGoodsRowCannotBeSplitIntoTwoTasks() throws Exception {
        seedApprovedLogistics("13900001003");
        seedApprovedShipper("13900001004");
        String shipperToken = login("13900001004", DEFAULT_PASSWORD);
        String logisticsToken = login("13900001003", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);

        splitTask(logisticsToken, orderId, List.of(goodsIds.get(0)));
        postJson("/api/logistics/waybills/" + orderId + "/tasks", logisticsToken,
                Map.of("goodsIds", List.of(goodsIds.get(0))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物已被其他任务占用")));

        postJson("/api/logistics/waybills/" + orderId + "/tasks", logisticsToken,
                Map.of("goodsIds", List.of(999999L)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物不属于该订单")));
    }

    @Test
    void otherLogisticsCannotViewOrSplitWaybill() throws Exception {
        seedApprovedLogistics("13900001005");
        seedApprovedLogistics("13900001006");
        seedApprovedShipper("13900001007");
        String shipperToken = login("13900001007", DEFAULT_PASSWORD);
        String ownerToken = login("13900001005", DEFAULT_PASSWORD);
        String otherToken = login("13900001006", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(ownerToken, orderId);
        List<Long> goodsIds = listGoodsIds(ownerToken, orderId);

        getJson("/api/logistics/waybills/" + orderId, otherToken)
                .andExpect(status().isForbidden());
        postJson("/api/logistics/waybills/" + orderId + "/tasks", otherToken,
                Map.of("goodsIds", List.of(goodsIds.get(0))))
                .andExpect(status().isForbidden());

        assertEquals(0, data(getJson("/api/logistics/waybills", otherToken, Map.of("pageSize", "10")))
                .path("total").asLong());
        assertEquals("ACCEPTED", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());
    }
}
