package com.example.web.module.message;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 04：系统通知未读、会话未读、会话合法关系与消息文本校验的 HTTP 接口测试。
 */
class MessageCenterTest extends FreightApiTestBase {

    @Test
    void notificationAndConversationUnreadFollowReadState() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900003001");
        Long shipperAccountId = seedApprovedShipper("13900003002");
        String shipperToken = login("13900003002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900003001", DEFAULT_PASSWORD);
        long orderOne = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderOne);
        long orderTwo = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderTwo);

        JsonNode count = data(getJson("/api/messages/unread-count", shipperToken));
        assertEquals(2, count.path("notificationUnread").asInt());
        assertEquals(0, count.path("conversationUnread").asInt());
        assertEquals(2, count.path("total").asInt());

        JsonNode notifications = data(getJson("/api/messages/notifications", shipperToken,
                Map.of("pageNum", "1", "pageSize", "10")));
        assertEquals(2, notifications.path("total").asLong());
        JsonNode first = notifications.path("records").get(0);
        assertTrue(first.path("id").asLong() > 0);
        assertEquals("ORDER_ACCEPTED", first.path("type").asText());
        assertFalse(first.path("title").asText().isBlank());
        assertFalse(first.path("content").asText().isBlank());
        assertEquals(0, first.path("isRead").asInt());
        assertTrue(first.hasNonNull("createTime"));
        assertEquals(orderTwo, first.path("orderId").asLong());

        long firstNotificationId = first.path("id").asLong();
        long secondNotificationId = notifications.path("records").get(1).path("id").asLong();
        postJson("/api/messages/notifications/" + firstNotificationId + "/read", shipperToken, Map.of())
                .andExpect(status().isOk());
        postJson("/api/messages/notifications/" + firstNotificationId + "/read", shipperToken, Map.of())
                .andExpect(status().isOk());
        assertEquals(1, data(getJson("/api/messages/unread-count", shipperToken)).path("notificationUnread").asInt());
        postJson("/api/messages/notifications/" + secondNotificationId + "/read", logisticsToken, Map.of())
                .andExpect(status().isForbidden());

        long conversationId = createConversation(shipperToken, logisticsAccountId, orderOne);
        sendMessage(shipperToken, conversationId, "第一车已到装货点");
        sendMessage(shipperToken, conversationId, "第二车请稍等");

        JsonNode logisticsCount = data(getJson("/api/messages/unread-count", logisticsToken));
        assertEquals(2, logisticsCount.path("conversationUnread").asInt());
        assertEquals(2, logisticsCount.path("total").asInt());

        JsonNode conversations = data(getJson("/api/messages/conversations", logisticsToken));
        assertEquals(1, conversations.size());
        JsonNode conversation = conversations.get(0);
        assertEquals(conversationId, conversation.path("conversationId").asLong());
        assertEquals(shipperAccountId, conversation.path("targetAccountId").asLong());
        assertEquals("SHIPPER", conversation.path("targetRole").asText());
        assertFalse(conversation.path("targetName").asText().isBlank());
        assertEquals(orderOne, conversation.path("relatedOrderId").asLong());
        assertEquals("第二车请稍等", conversation.path("lastMessageContent").asText());
        assertTrue(conversation.hasNonNull("lastMessageTime"));
        assertEquals(2, conversation.path("unreadCount").asInt());

        JsonNode detail = data(getJson("/api/messages/conversations/" + conversationId, logisticsToken,
                Map.of("pageNum", "1", "pageSize", "20")));
        assertEquals(2, detail.path("total").asLong());
        JsonNode firstMessage = detail.path("records").get(0);
        assertEquals("第一车已到装货点", firstMessage.path("content").asText());
        assertEquals(1, firstMessage.path("isRead").asInt());
        assertTrue(firstMessage.hasNonNull("readTime"));
        String firstReadTime = firstMessage.path("readTime").asText();
        assertEquals(0, data(getJson("/api/messages/unread-count", logisticsToken))
                .path("conversationUnread").asInt());

        JsonNode reopened = data(getJson("/api/messages/conversations/" + conversationId, logisticsToken,
                Map.of("pageNum", "1", "pageSize", "20")));
        assertEquals(firstReadTime, reopened.path("records").get(0).path("readTime").asText());
        assertEquals(0, data(getJson("/api/messages/unread-count", logisticsToken))
                .path("conversationUnread").asInt());

        sendMessage(logisticsToken, conversationId, "收到，注意安全");
        assertEquals(1, data(getJson("/api/messages/unread-count", shipperToken))
                .path("conversationUnread").asInt());
        JsonNode shipperDetail = data(getJson("/api/messages/conversations/" + conversationId, shipperToken,
                Map.of("pageNum", "1", "pageSize", "20")));
        assertEquals(3, shipperDetail.path("total").asLong());
        assertEquals(1, shipperDetail.path("records").get(2).path("isRead").asInt());
        assertEquals(0, data(getJson("/api/messages/unread-count", shipperToken))
                .path("conversationUnread").asInt());
    }

    @Test
    void conversationCreationValidatesBusinessRelationships() throws Exception {
        Long logisticsA = seedApprovedLogistics("13900003011");
        Long logisticsB = seedApprovedLogistics("13900003012");
        Long logisticsC = seedApprovedLogistics("13900003013");
        Long shipper = seedApprovedShipper("13900003014");
        seedApprovedShipper("13900003015");
        String shipperToken = login("13900003014", DEFAULT_PASSWORD);
        String logisticsBToken = login("13900003012", DEFAULT_PASSWORD);
        String logisticsCToken = login("13900003013", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        Long driverVehicleA = seedVehicle(logisticsA, "京A30101");
        Long driverAccountA = seedDriver(logisticsA, driverVehicleA, "13900003016");
        Long driverVehicleB = seedVehicle(logisticsB, "京B30102");
        Long driverAccountB = seedDriver(logisticsB, driverVehicleB, "13900003017");
        String driverTokenA = login("13900003016", DEFAULT_PASSWORD);
        String driverTokenB = login("13900003017", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsBToken, orderId);

        long driverCompanyConversation = createConversation(driverTokenA, logisticsA, null);
        assertTrue(driverCompanyConversation > 0);
        long shipperAcceptedConversation = createConversation(shipperToken, logisticsB, orderId);
        assertTrue(shipperAcceptedConversation > 0);

        createConversationRejected(driverTokenA, logisticsB, null);
        createConversationRejected(driverTokenA, shipper, null);
        createConversationRejected(driverTokenA, driverAccountB, null);
        createConversationRejected(shipperToken, logisticsC, null);
        createConversationRejected(shipperToken, driverAccountA, null);
        createConversationRejected(shipperToken, accountIdOf("13900003015"), null);
        createConversationRejected(driverTokenB, driverAccountA, null);

        assertTrue(createConversation(adminToken, driverAccountA, null) > 0);
        assertTrue(createConversation(adminToken, shipper, null) > 0);
        assertTrue(createConversation(adminToken, logisticsB, null) > 0);
        createConversationRejected(adminToken, accountIdOf(ADMIN_PHONE), null);

        getJson("/api/messages/conversations/" + driverCompanyConversation, logisticsCToken)
                .andExpect(status().isForbidden());
        getJson("/api/messages/conversations/" + driverCompanyConversation, driverTokenB)
                .andExpect(status().isForbidden());
        sendMessageRejected(logisticsCToken, driverCompanyConversation, "越权发送");
    }

    @Test
    void messageTextIsLimitedAndConversationIsReused() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900003021");
        seedApprovedShipper("13900003022");
        seedApprovedLogistics("13900003023");
        Long vehicleId = seedVehicle(logisticsAccountId, "京A30201");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900003024");
        String driverToken = login("13900003024", DEFAULT_PASSWORD);
        String logisticsToken = login("13900003021", DEFAULT_PASSWORD);
        String otherLogisticsToken = login("13900003023", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        long firstConversation = createConversation(driverToken, logisticsAccountId, null);
        long secondConversation = createConversation(driverToken, logisticsAccountId, null);
        assertEquals(firstConversation, secondConversation);

        postJson("/api/messages/conversations/" + firstConversation + "/send", driverToken,
                Map.of("content", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不能为空")));
        postJson("/api/messages/conversations/" + firstConversation + "/send", driverToken,
                Map.of("content", "a".repeat(1001)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("1000")));
        postJson("/api/messages/conversations/" + firstConversation + "/send", driverToken,
                Map.of("content", "a".repeat(1000)))
                .andExpect(status().isOk());

        createConversationRejected(driverToken, driverAccountId, null);

        JsonNode unread = data(getJson("/api/messages/unread-count", driverToken));
        assertEquals(0, unread.path("notificationUnread").asInt(), "发送站内消息不应产生系统通知");
        assertEquals(0, unread.path("conversationUnread").asInt());

        getJson("/admin/messages", driverToken).andExpect(status().isForbidden());
        JsonNode adminMessages = data(getJson("/admin/messages", adminToken,
                Map.of("pageNum", "1", "pageSize", "10")));
        assertTrue(adminMessages.path("total").asLong() >= 1);
        JsonNode record = adminMessages.path("records").get(0);
        assertEquals(firstConversation, record.path("conversationId").asLong());
        assertEquals("司机13900003024", record.path("senderName").asText());
        assertEquals("DRIVER", record.path("senderRole").asText());
        assertEquals("物流13900003021", record.path("receiverName").asText());
        assertEquals("LOGISTICS", record.path("receiverRole").asText());
        assertEquals("a".repeat(1000), record.path("content").asText());
        assertTrue(record.hasNonNull("sendTime"));
        assertEquals(0, record.path("isRead").asInt());

        sendMessageRejected(otherLogisticsToken, firstConversation, "越权发送");
    }

    private long createConversation(String token, Long targetAccountId, Long orderId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetAccountId", targetAccountId);
        body.put("orderId", orderId);
        JsonNode result = data(postJson("/api/messages/conversations", token, body));
        return result.asLong();
    }

    private void createConversationRejected(String token, Long targetAccountId, Long orderId) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("targetAccountId", targetAccountId);
        body.put("orderId", orderId);
        postJson("/api/messages/conversations", token, body)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("无权与该账号发起会话")));
    }

    private void sendMessage(String token, long conversationId, String content) throws Exception {
        postJson("/api/messages/conversations/" + conversationId + "/send", token, Map.of("content", content))
                .andExpect(status().isOk());
    }

    private void sendMessageRejected(String token, long conversationId, String content) throws Exception {
        postJson("/api/messages/conversations/" + conversationId + "/send", token, Map.of("content", content))
                .andExpect(status().isForbidden());
    }

    private Long accountIdOf(String phone) {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_account WHERE username = ?", Long.class, phone);
    }
}
