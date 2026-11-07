package com.example.web.module.order;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.module.order.service.OrderReminderService;
import com.example.web.module.task.entity.Task;
import com.example.web.module.task.enums.TaskStatusEnum;
import com.example.web.module.task.mapper.TaskMapper;
import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 票据 13：撤回、取消边界、到期提醒、自动下架与一键重发的 HTTP 接口测试。
 */
class OrderLifecycleTest extends ApiTestBase {

    @Autowired
    private OrderReminderService orderReminderService;

    @Autowired
    private TaskMapper taskMapper;

    @Test
    void withdrawOnlyAllowedWhilePendingAccept() throws Exception {
        seedApprovedShipper("13900000301");
        seedApprovedLogistics("13900000302");
        String shipperToken = login("13900000301", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000302", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken);
        postJson("/api/shipper/orders/" + orderId + "/withdraw", shipperToken, Map.of())
                .andExpect(status().isOk());

        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("WITHDRAWN", detail.path("orderStatus").asText());
        assertTrue(detail.hasNonNull("withdrawTime"));
        assertEquals(0, data(getJson("/api/logistics/order-pool", logisticsToken, Map.of("pageSize", "10"))
                ).path("total").asLong());

        postJson("/api/shipper/orders/" + orderId + "/withdraw", shipperToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("待接单")));
    }

    @Test
    void cancelTerminatesUndepartedTasksAndNotifiesRelatedParties() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900000304");
        seedApprovedShipper("13900000303");
        String shipperToken = login("13900000303", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000304", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A33001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900000305");

        long orderId = publishOrder(shipperToken);
        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());

        seedTask(orderId, logisticsAccountId, TaskStatusEnum.WAIT_EXECUTE, driverAccountId);
        seedTask(orderId, logisticsAccountId, TaskStatusEnum.PENDING_ASSIGN, null);

        postJson("/api/shipper/orders/" + orderId + "/cancel", shipperToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("取消原因不能为空")));

        postJson("/api/shipper/orders/" + orderId + "/cancel", shipperToken, Map.of("reason", "客户临时取消运输"))
                .andExpect(status().isOk());

        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("CANCELED", detail.path("orderStatus").asText());
        assertEquals("客户临时取消运输", detail.path("cancelReason").asText());
        assertTrue(detail.hasNonNull("cancelTime"));
        assertTrue(detail.hasNonNull("cancelByAccountId"));

        assertEquals(2L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task WHERE order_id = ? AND task_status = 'TERMINATED'",
                Long.class, orderId));
        assertEquals(3L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'ORDER_CANCELED'",
                Long.class, orderId));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = 'ORDER_CANCELED'"
                        + " AND receiver_account_id = ?",
                Long.class, orderId, driverAccountId));
    }

    @Test
    void cancelRejectedOnceAnyTaskDepartedOrCompleted() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900000306");
        seedApprovedShipper("13900000307");
        String shipperToken = login("13900000307", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000306", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken);
        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());
        seedTask(orderId, logisticsAccountId, TaskStatusEnum.IN_TRANSPORT, null);

        postJson("/api/shipper/orders/" + orderId + "/cancel", shipperToken, Map.of("reason", "不再运输"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务已出发，不能取消订单")));

        assertEquals("ACCEPTED", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task WHERE order_id = ? AND task_status = 'IN_TRANSPORT'",
                Long.class, orderId));
    }

    @Test
    void logisticsCanCancelOwnAcceptedOrderBeforeDeparture() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900000315");
        seedApprovedLogistics("13900000317");
        seedApprovedShipper("13900000316");
        String shipperToken = login("13900000316", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000315", DEFAULT_PASSWORD);
        String otherLogisticsToken = login("13900000317", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken);
        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());
        seedTask(orderId, logisticsAccountId, TaskStatusEnum.PENDING_ASSIGN, null);

        postJson("/api/logistics/orders/" + orderId + "/cancel", otherLogisticsToken,
                Map.of("reason", "非本公司订单"))
                .andExpect(status().isForbidden());
        postJson("/api/logistics/orders/" + orderId + "/cancel", shipperToken,
                Map.of("reason", "角色越权"))
                .andExpect(status().isForbidden());

        postJson("/api/logistics/orders/" + orderId + "/cancel", logisticsToken,
                Map.of("reason", "车辆临时故障"))
                .andExpect(status().isOk());

        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("CANCELED", detail.path("orderStatus").asText());
        assertEquals("车辆临时故障", detail.path("cancelReason").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_task WHERE order_id = ? AND task_status = 'TERMINATED'",
                Long.class, orderId));
    }

    @Test
    void remindersAreIdempotentAndExpiredPendingOrderIsOffshelved() throws Exception {
        seedApprovedShipper("13900000308");
        seedApprovedLogistics("13900000309");
        String shipperToken = login("13900000308", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000309", DEFAULT_PASSWORD);

        clock.setNow(LocalDateTime.of(2026, 11, 7, 8, 0));
        long orderId = publishOrder(shipperToken);

        clock.advanceMinutes(192 * 60);
        orderReminderService.scanOrders();
        assertEquals("PENDING_ACCEPT", orderStatus(shipperToken, orderId));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_2D"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_2D"));
        assertEquals(0L, reminderCount(orderId, "T_MINUS_1D"));
        assertEquals(0L, reminderCount(orderId, "T_MINUS_1H"));

        clock.advanceMinutes(24 * 60);
        orderReminderService.scanOrders();
        assertEquals(1L, reminderCount(orderId, "T_MINUS_2D"));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_1D"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_1D"));

        clock.advanceMinutes(23 * 60);
        orderReminderService.scanOrders();
        assertEquals(1L, reminderCount(orderId, "T_MINUS_2D"));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_1D"));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_1H"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_1H"));

        clock.advanceMinutes(60);
        orderReminderService.scanOrders();
        JsonNode expired = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("OFFSHELVED", expired.path("orderStatus").asText());
        assertTrue(expired.hasNonNull("offshelfTime"));
        assertEquals(1L, reminderCount(orderId, "AUTO_OFFSHELF"));
        assertEquals(1L, noticeCount(orderId, "ORDER_OFFSHELF"));
        assertEquals(0, data(getJson("/api/logistics/order-pool", logisticsToken, Map.of("pageSize", "10"))
                ).path("total").asLong());

        orderReminderService.scanOrders();
        orderReminderService.scanOrders();
        assertEquals(1L, reminderCount(orderId, "T_MINUS_2D"));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_1D"));
        assertEquals(1L, reminderCount(orderId, "T_MINUS_1H"));
        assertEquals(1L, reminderCount(orderId, "AUTO_OFFSHELF"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_2D"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_1D"));
        assertEquals(1L, noticeCount(orderId, "ORDER_REMIND_1H"));
        assertEquals(1L, noticeCount(orderId, "ORDER_OFFSHELF"));
    }

    @Test
    void acceptedOrderIsNotOffshelvedWhenExpired() throws Exception {
        seedApprovedShipper("13900000310");
        seedApprovedLogistics("13900000311");
        String shipperToken = login("13900000310", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000311", DEFAULT_PASSWORD);

        clock.setNow(LocalDateTime.of(2026, 11, 7, 8, 0));
        long orderId = publishOrder(shipperToken);
        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());

        clock.advanceMinutes(240 * 60 + 1);
        orderReminderService.scanOrders();
        assertEquals("ACCEPTED", orderStatus(shipperToken, orderId));
        assertEquals(0L, reminderCount(orderId, "AUTO_OFFSHELF"));
        assertEquals(0L, reminderCount(orderId, "T_MINUS_2D"));
        assertEquals(0L, reminderCount(orderId, "T_MINUS_1D"));
        assertEquals(0L, reminderCount(orderId, "T_MINUS_1H"));
    }

    @Test
    void republishCreatesNewOrderFromOffshelvedOrderOnly() throws Exception {
        seedApprovedShipper("13900000312");
        String shipperToken = login("13900000312", DEFAULT_PASSWORD);

        clock.setNow(LocalDateTime.of(2026, 11, 7, 8, 0));
        long orderId = publishOrder(shipperToken);
        clock.advanceMinutes(240 * 60 + 1);
        orderReminderService.scanOrders();
        assertEquals("OFFSHELVED", orderStatus(shipperToken, orderId));

        JsonNode republished = data(postJson("/api/shipper/orders/" + orderId + "/republish", shipperToken,
                Map.of()));
        long newOrderId = republished.path("id").asLong();
        assertTrue(newOrderId != orderId);
        assertEquals("PENDING_ACCEPT", republished.path("orderStatus").asText());

        JsonNode newDetail = data(getJson("/api/shipper/orders/" + newOrderId, shipperToken));
        assertEquals(orderId, newDetail.path("sourceOrderId").asLong());
        assertEquals(1, newDetail.path("republishCount").asInt());
        assertEquals(1, newDetail.path("goods").size());
        assertEquals("张三", newDetail.path("pickup").path("contactName").asText());
        assertEquals(240, newDetail.path("validHours").asInt());
        assertEquals("PENDING_ACCEPT", newDetail.path("orderStatus").asText());

        JsonNode oldDetail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("OFFSHELVED", oldDetail.path("orderStatus").asText());
        assertNotEquals(oldDetail.path("orderNo").asText(), newDetail.path("orderNo").asText());
        assertEquals(0, oldDetail.path("republishCount").asInt());

        JsonNode mine = data(getJson("/api/shipper/orders", shipperToken, Map.of("pageSize", "10")));
        assertEquals(2, mine.path("total").asLong());
    }

    @Test
    void withdrawnAndCanceledOrdersCannotBeRepublished() throws Exception {
        seedApprovedShipper("13900000313");
        seedApprovedLogistics("13900000314");
        String shipperToken = login("13900000313", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000314", DEFAULT_PASSWORD);

        long withdrawnId = publishOrder(shipperToken);
        postJson("/api/shipper/orders/" + withdrawnId + "/withdraw", shipperToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/shipper/orders/" + withdrawnId + "/republish", shipperToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("仅已下架订单")));

        long canceledId = publishOrder(shipperToken);
        postJson("/api/logistics/order-pool/" + canceledId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/shipper/orders/" + canceledId + "/cancel", shipperToken, Map.of("reason", "取消"))
                .andExpect(status().isOk());
        postJson("/api/shipper/orders/" + canceledId + "/republish", shipperToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("仅已下架订单")));
    }

    private String orderStatus(String token, long orderId) throws Exception {
        return data(getJson("/api/shipper/orders/" + orderId, token)).path("orderStatus").asText();
    }

    private long reminderCount(long orderId, String reminderType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_reminder_record WHERE order_id = ? AND reminder_type = ?",
                Long.class, orderId, reminderType);
    }

    private long noticeCount(long orderId, String noticeType) {
        return jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE order_id = ? AND notice_type = ?",
                Long.class, orderId, noticeType);
    }

    private Long seedTask(long orderId, Long logisticsAccountId, TaskStatusEnum taskStatus, Long driverAccountId) {
        Task task = new Task();
        task.setTaskNo("TK" + System.nanoTime());
        task.setOrderId(orderId);
        task.setLogisticsAccountId(logisticsAccountId);
        task.setTaskStatus(taskStatus.getCode());
        task.setDriverAccountId(driverAccountId);
        task.setOpenClaim(0);
        task.setDriverAck(0);
        taskMapper.insert(task);
        return task.getId();
    }

    private long publishOrder(String shipperToken) throws Exception {
        long orderId = data(postJson("/api/shipper/orders/save", shipperToken, orderBody())).path("id").asLong();
        postJson("/api/shipper/orders/" + orderId + "/publish", shipperToken, Map.of())
                .andExpect(status().isOk());
        return orderId;
    }

    private Map<String, Object> orderBody() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("freightAmount", 12000.0);
        body.put("pickupTime", "2026-12-08 08:00:00");
        body.put("deliveryTime", "2026-12-09 18:00:00");
        body.put("remark", "发货备注");
        Map<String, Object> goods = new LinkedHashMap<>();
        goods.put("goodsName", "钢材");
        goods.put("goodsType", "STEEL");
        goods.put("quantity", 20);
        goods.put("weight", 30);
        goods.put("volume", 12);
        goods.put("unit", "件");
        body.put("goods", List.of(goods));
        body.put("pickup", address("北京市", "北京市", "朝阳区", "装货详细地址", "张三", "13900000001"));
        body.put("delivery", address("广东省", "广州市", "天河区", "卸货详细地址", "收货人", "13700000000"));
        return body;
    }

    private Map<String, Object> address(String province, String city, String district, String detail,
                                        String contactName, String contactPhone) {
        Map<String, Object> address = new LinkedHashMap<>();
        address.put("province", province);
        address.put("city", city);
        address.put("district", district);
        address.put("detail", detail);
        address.put("contactName", contactName);
        address.put("contactPhone", contactPhone);
        return address;
    }
}
