package com.example.web.acceptance;

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
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
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
 * 票据 17：全链路验收路径，覆盖入驻审核、发布、并发抢单、拆单派车、打卡、报销、消息与边界规则。
 */
class EndToEndAcceptanceTest extends FreightApiTestBase {

    @Test
    void fullChainAcceptanceCoversFreightCheckinReimbursementAndMessages() throws Exception {
        clock.setNow(LocalDateTime.of(2026, 11, 8, 9, 0));
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        // 一、管理员审核货主与两家物流公司入驻
        register("SHIPPER", "13900008001", "验收货主", "shipper123");
        String shipperToken = login("13900008001", "shipper123");
        submitRealname(shipperToken, "验收货主", "110101199001018001", "13900008001");
        long shipperOnboardingId = submitOnboarding(shipperToken, "北京验收货主企业",
                "911101010808000801", "验收法人甲", "110101198001018002", "13900008001");
        approveOnboarding(adminToken, shipperOnboardingId);
        JsonNode shipperProfile = data(getJson("/api/auth/profile", shipperToken));
        assertEquals("PASSED", shipperProfile.path("realnameStatus").asText());
        assertEquals("APPROVED", shipperProfile.path("onboardingStatus").asText());
        assertEquals("APPROVED", data(getJson("/api/onboarding", shipperToken)).path("auditStatus").asText());

        LogisticsFixture logisticsA = onboardLogistics(adminToken, "13900008002", "验收物流甲",
                "logistics123", "110101199001018003", "911101010808000802", "验收法人乙", "110101198001018004");
        LogisticsFixture logisticsB = onboardLogistics(adminToken, "13900008003", "验收物流乙",
                "logistics123", "110101199001018005", "911101010808000803", "验收法人丙", "110101198001018006");
        assertEquals("APPROVED", data(getJson("/api/onboarding", logisticsA.token)).path("auditStatus").asText());
        assertEquals("APPROVED", data(getJson("/api/onboarding", logisticsB.token)).path("auditStatus").asText());

        // 二、物流公司创建车辆与司机，司机首次登录强制改密后重新登录
        createVehicleAndDriver(logisticsA, "京A80001", "LIC80001", "验收司机甲", "13900008004",
                "init123", "driver123", "110101199001018007", "DRV80001");
        createVehicleAndDriver(logisticsB, "京B80002", "LIC80002", "验收司机乙", "13900008005",
                "init456", "driver456", "110101199001018008", "DRV80002");

        // 三、货主保存草稿并发布，订单池隐藏隐私字段后两家物流公司并发接单
        long orderId = saveOrderDraft(shipperToken, 2);
        postJson("/api/shipper/orders/" + orderId + "/publish", shipperToken, Map.of())
                .andExpect(status().isOk());
        JsonNode pool = data(getJson("/api/logistics/order-pool", logisticsA.token, Map.of("pageSize", "10")));
        assertEquals(1, pool.path("total").asLong());
        String poolJson = pool.toString();
        assertFalse(poolJson.contains("contactName"));
        assertFalse(poolJson.contains("contactPhone"));
        assertFalse(poolJson.contains("\"detail\""));
        assertFalse(poolJson.contains("remark"));
        JsonNode poolDetail = data(getJson("/api/logistics/order-pool/" + orderId, logisticsA.token));
        assertFalse(poolDetail.toString().contains("contactName"));
        assertFalse(poolDetail.toString().contains("\"detail\""));

        String[] raceResults = raceAccept("/api/logistics/order-pool/" + orderId + "/accept",
                logisticsA.token, logisticsB.token);
        int firstStatus = Integer.parseInt(raceResults[0].substring(0, raceResults[0].indexOf('|')));
        int secondStatus = Integer.parseInt(raceResults[1].substring(0, raceResults[1].indexOf('|')));
        assertEquals(1, (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0),
                "两家物流公司并发接单只能一家成功");
        String conflictBody = firstStatus == 400 ? raceResults[0] : raceResults[1];
        assertTrue(conflictBody.contains("已被接走"));

        JsonNode acceptedOrder = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("ACCEPTED", acceptedOrder.path("orderStatus").asText());
        long winnerAccountId = acceptedOrder.path("logisticsAccountId").asLong();
        LogisticsFixture winner = winnerAccountId == logisticsA.accountId ? logisticsA : logisticsB;
        LogisticsFixture loser = winner == logisticsA ? logisticsB : logisticsA;
        JsonNode waybill = data(getJson("/api/logistics/waybills/" + orderId, winner.token));
        assertEquals("装货详细地址", waybill.path("pickup").path("detail").asText());
        assertEquals("卸货详细地址", waybill.path("delivery").path("detail").asText());
        assertFalse(acceptedOrder.path("logisticsName").asText().isBlank());

        // 四、拆单并分别指派与自抢，领取前任务池只展示概要
        List<Long> goodsIds = listGoodsIds(winner.token, orderId);
        assertEquals(2, goodsIds.size());
        long firstTask = splitTask(winner.token, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(winner.token, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(winner.token, firstTask, winner.driverId, winner.vehicleId);
        postJson("/api/logistics/tasks/" + secondTask + "/open-claim", winner.token, Map.of())
                .andExpect(status().isOk());
        JsonNode taskPool = data(getJson("/api/driver/task-pool", winner.driverToken, Map.of("pageSize", "10")));
        assertEquals(1, taskPool.path("total").asLong());
        String taskPoolJson = taskPool.toString();
        assertFalse(taskPoolJson.contains("contactName"));
        assertFalse(taskPoolJson.contains("contactPhone"));
        assertFalse(taskPoolJson.contains("\"detail\""));
        assertFalse(taskPoolJson.contains("remark"));
        JsonNode claimed = data(postJson("/api/driver/task-pool/" + secondTask + "/claim",
                winner.driverToken, Map.of()));
        assertEquals("WAIT_EXECUTE", claimed.path("taskStatus").asText());
        assertEquals("CLAIMED", claimed.path("assignType").asText());
        JsonNode claimedDetail = data(getJson("/api/driver/tasks/" + secondTask, winner.driverToken));
        assertEquals("卸货详细地址", claimedDetail.path("delivery").path("detail").asText());
        assertEquals(0, data(getJson("/api/logistics/waybills/" + orderId, winner.token))
                .path("progress").path("unassignedGoods").asInt());

        // 五、司机知悉确认、出发打卡与到达打卡
        String loadPhoto = seedFile(winner.driverAccountId, "accept-load.jpg");
        String vehiclePhoto = seedFile(winner.driverAccountId, "accept-vehicle.jpg");
        String unloadPhoto = seedFile(winner.driverAccountId, "accept-unload.jpg");
        String signPhoto = seedFile(winner.driverAccountId, "accept-sign.jpg");
        String secondLoadPhoto = seedFile(winner.driverAccountId, "accept-load-2.jpg");
        String secondVehiclePhoto = seedFile(winner.driverAccountId, "accept-vehicle-2.jpg");
        String secondUnloadPhoto = seedFile(winner.driverAccountId, "accept-unload-2.jpg");
        String secondSignPhoto = seedFile(winner.driverAccountId, "accept-sign-2.jpg");

        postJson("/api/driver/tasks/" + firstTask + "/ack", winner.driverToken, Map.of())
                .andExpect(status().isOk());
        clock.setNow(LocalDateTime.of(2026, 11, 8, 10, 0));
        postJson("/api/driver/tasks/" + firstTask + "/checkin/depart", winner.driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());
        JsonNode departedTask = data(getJson("/api/driver/tasks/" + firstTask, winner.driverToken));
        assertEquals("IN_TRANSPORT", departedTask.path("taskStatus").asText());
        assertEquals(1, departedTask.path("driverAck").asInt());
        assertEquals("IN_PROGRESS", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());

        clock.setNow(LocalDateTime.of(2026, 11, 8, 11, 0));
        postJson("/api/driver/tasks/" + firstTask + "/checkin/arrive", winner.driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto))))
                .andExpect(status().isOk());
        assertEquals("COMPLETED", data(getJson("/api/driver/tasks/" + firstTask, winner.driverToken))
                .path("taskStatus").asText());
        assertEquals("IN_PROGRESS", data(getJson("/api/shipper/orders/" + orderId, shipperToken))
                .path("orderStatus").asText());

        // 六、任务二超窗出发仍可提交，锁定后不可重复提交，到达后运单收口
        clock.setNow(LocalDateTime.of(2026, 12, 8, 9, 0));
        JsonNode lateDepart = data(postJson("/api/driver/tasks/" + secondTask + "/checkin/depart",
                winner.driverToken, checkinBody(116.4, 39.9,
                        List.of(photo("LOAD_DONE", secondLoadPhoto),
                                photo("VEHICLE_STATE", secondVehiclePhoto)))));
        assertEquals(1, lateDepart.path("versionNo").asInt());
        assertEquals(0, lateDepart.path("onTime").asInt(), "超过约定时间 20 分钟应标记超窗");
        clock.advanceMinutes(21);
        postJson("/api/driver/tasks/" + secondTask + "/checkin/depart", winner.driverToken,
                checkinBody(116.5, 39.95, List.of(photo("LOAD_DONE", secondLoadPhoto),
                        photo("VEHICLE_STATE", secondVehiclePhoto))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("打卡已锁定")));

        clock.setNow(LocalDateTime.of(2026, 12, 9, 19, 0));
        JsonNode lateArrive = data(postJson("/api/driver/tasks/" + secondTask + "/checkin/arrive",
                winner.driverToken, checkinBody(113.3, 23.1,
                        List.of(photo("UNLOAD_DONE", secondUnloadPhoto),
                                photo("SIGN_RECEIPT", secondSignPhoto)))));
        assertEquals(0, lateArrive.path("onTime").asInt());
        JsonNode completedOrder = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("COMPLETED", completedOrder.path("orderStatus").asText());
        assertEquals(2, completedOrder.path("tasks").size());
        for (JsonNode task : completedOrder.path("tasks")) {
            assertTaskCheckinVisible(task);
        }
        JsonNode completedWaybill = data(getJson("/api/logistics/waybills/" + orderId, winner.token));
        assertEquals("COMPLETED", completedWaybill.path("orderStatus").asText());
        assertEquals(2, completedWaybill.path("progress").path("completedCount").asInt());
        assertEquals(2, completedWaybill.path("progress").path("arriveCount").asInt());
        for (JsonNode task : completedWaybill.path("tasks")) {
            assertTaskCheckinVisible(task);
        }

        JsonNode records = data(getJson("/api/driver/records", winner.driverToken,
                Map.of("taskId", String.valueOf(secondTask), "node", "ARRIVE", "pageSize", "10")));
        assertEquals(1, records.path("total").asLong());
        JsonNode record = records.path("records").get(0);
        assertEquals("ARRIVE", record.path("node").asText());
        assertEquals(0, record.path("onTime").asInt());
        assertFalse(record.path("waybillNo").asText().isBlank());
        assertTrue(containsPurpose(record.path("photos"), "UNLOAD_DONE"));
        assertTrue(containsPurpose(record.path("photos"), "SIGN_RECEIPT"));

        // 七、司机提交报销，正常单审核通过，超限单必须先特殊复核
        clock.setNow(LocalDateTime.of(2026, 12, 20, 10, 0));
        String receipt = seedFile(winner.driverAccountId, "accept-receipt.jpg");
        long normalReimbursement = data(postJson("/api/driver/reimbursements", winner.driverToken,
                reimbursementBody(firstTask, 4000.0, "FUEL", "2026-12-08", receipt))).path("id").asLong();
        postJson("/api/logistics/reimbursements/" + normalReimbursement + "/review", winner.token,
                Map.of("action", "APPROVE", "note", "费用合理"))
                .andExpect(status().isOk());
        assertEquals("APPROVED", data(getJson("/api/logistics/reimbursements/" + normalReimbursement,
                winner.token)).path("auditStatus").asText());

        JsonNode overspend = data(postJson("/api/driver/reimbursements", winner.driverToken,
                reimbursementBody(firstTask, 1500.0, "TOLL", "2026-12-09", receipt)));
        assertEquals(1, overspend.path("overspend").asInt());
        assertEquals(5500.0, overspend.path("totalSnapshot").asDouble(), 0.001);
        long overspendId = overspend.path("id").asLong();
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", winner.token,
                Map.of("action", "APPROVE", "note", "直接通过"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("特殊复核")));
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", winner.token,
                Map.of("action", "SPECIAL_REVIEW", "note", "超限属实，同意报销"))
                .andExpect(status().isOk());
        postJson("/api/logistics/reimbursements/" + overspendId + "/review", winner.token,
                Map.of("action", "APPROVE", "note", "复核通过"))
                .andExpect(status().isOk());
        JsonNode quota = data(getJson("/api/driver/reimbursements/quota", winner.driverToken,
                Map.of("yearMonth", "2026-12")));
        assertEquals(5500.0, quota.path("used").asDouble(), 0.001);

        // 八、货主与接单物流公司可收发消息，无关公司发起被拒，未读与已读状态正确
        long conversationId = data(postJson("/api/messages/conversations", shipperToken,
                Map.of("targetAccountId", winner.accountId, "orderId", orderId))).asLong();
        postJson("/api/messages/conversations/" + conversationId + "/send", shipperToken,
                Map.of("content", "提货时间已确认，请按时到达装货点"))
                .andExpect(status().isOk());
        assertEquals(1, data(getJson("/api/messages/unread-count", winner.token))
                .path("conversationUnread").asInt());
        JsonNode conversation = data(getJson("/api/messages/conversations/" + conversationId,
                winner.token, Map.of("pageNum", "1", "pageSize", "10")));
        assertEquals(1, conversation.path("total").asLong());
        assertEquals(1, conversation.path("records").get(0).path("isRead").asInt());
        assertEquals(0, data(getJson("/api/messages/unread-count", winner.token))
                .path("conversationUnread").asInt());
        postJson("/api/messages/conversations", shipperToken,
                Map.of("targetAccountId", loser.accountId, "orderId", orderId))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("无权与该账号发起会话")));
        JsonNode adminMessages = data(getJson("/admin/messages", adminToken,
                Map.of("pageNum", "1", "pageSize", "10")));
        assertTrue(adminMessages.path("total").asLong() >= 1);
        assertEquals("提货时间已确认，请按时到达装货点",
                adminMessages.path("records").get(0).path("content").asText());
        assertEquals("SHIPPER", adminMessages.path("records").get(0).path("senderRole").asText());
        assertEquals("LOGISTICS", adminMessages.path("records").get(0).path("receiverRole").asText());
    }

    @Test
    void boundaryRulesAcceptanceCoversAbandonReclaimAndCancel() throws Exception {
        clock.setNow(LocalDateTime.of(2026, 11, 8, 9, 0));
        Long logisticsAccountId = seedApprovedLogistics("13900008021");
        seedApprovedShipper("13900008022");
        String shipperToken = login("13900008022", DEFAULT_PASSWORD);
        String logisticsToken = login("13900008021", DEFAULT_PASSWORD);
        Long vehicleId = seedVehicle(logisticsAccountId, "京A80021");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900008023");
        String driverToken = login("13900008023", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);

        // 未出发任务可弃单与回收，全部未出发时订单可取消
        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, firstTask, driverId, vehicleId);
        dispatchTask(logisticsToken, secondTask, driverId, vehicleId);

        JsonNode abandoned = data(postJson("/api/driver/tasks/" + firstTask + "/abandon", driverToken,
                Map.of("reason", "临时无法到场")));
        assertEquals("PENDING_ASSIGN", abandoned.path("taskStatus").asText());
        JsonNode reclaimed = data(postJson("/api/logistics/tasks/" + secondTask + "/reclaim",
                logisticsToken, Map.of("reason", "车辆调度调整")));
        assertEquals("PENDING_ASSIGN", reclaimed.path("taskStatus").asText());

        postJson("/api/shipper/orders/" + orderId + "/cancel", shipperToken,
                Map.of("reason", "运力不足"))
                .andExpect(status().isOk());
        JsonNode canceledOrder = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("CANCELED", canceledOrder.path("orderStatus").asText());
        for (JsonNode task : canceledOrder.path("tasks")) {
            assertEquals("TERMINATED", task.path("taskStatus").asText());
        }

        // 任一任务出发后货主与物流公司均不可取消订单
        long activeOrderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, activeOrderId);
        long activeTask = splitTask(logisticsToken, activeOrderId,
                listGoodsIds(logisticsToken, activeOrderId)).path("taskId").asLong();
        dispatchTask(logisticsToken, activeTask, driverId, vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        postJson("/api/driver/tasks/" + activeTask + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(
                        photo("LOAD_DONE", seedFile(driverAccountId, "boundary-load.jpg")),
                        photo("VEHICLE_STATE", seedFile(driverAccountId, "boundary-vehicle.jpg")))))
                .andExpect(status().isOk());
        postJson("/api/shipper/orders/" + activeOrderId + "/cancel", shipperToken,
                Map.of("reason", "需求取消"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("不可取消")));
    }

    /**
     * 注册并完成物流公司实名、入驻与管理员审核。
     *
     * @param adminToken 管理员令牌
     * @param phone 物流公司手机号
     * @param name 主体名称
     * @param password 登录密码
     * @param personalIdNo 个人实名证件号
     * @param creditCode 统一社会信用代码
     * @param legalRepName 法定代表人姓名
     * @param legalRepIdNo 法定代表人证件号
     * @return 物流公司验收夹具
     */
    private LogisticsFixture onboardLogistics(String adminToken, String phone, String name, String password,
                                              String personalIdNo, String creditCode,
                                              String legalRepName, String legalRepIdNo) throws Exception {
        register("LOGISTICS", phone, name, password);
        String token = login(phone, password);
        submitRealname(token, name, personalIdNo, phone);
        long onboardingId = submitOnboarding(token, name + "企业", creditCode,
                legalRepName, legalRepIdNo, phone);
        approveOnboarding(adminToken, onboardingId);
        return new LogisticsFixture(token, data(getJson("/api/auth/profile", token))
                .path("accountId").asLong());
    }

    /**
     * 创建车辆与司机，验证首次登录强制改密并重新登录。
     *
     * @param fixture 物流公司夹具
     * @param plateNo 车牌号
     * @param vehicleLicenseNo 行驶证号
     * @param driverName 司机姓名
     * @param driverPhone 司机手机号
     * @param initialPassword 初始密码
     * @param newPassword 改密后的密码
     * @param driverIdNo 司机证件号
     * @param driverLicenseNo 驾驶证号
     */
    private void createVehicleAndDriver(LogisticsFixture fixture, String plateNo, String vehicleLicenseNo,
                                        String driverName, String driverPhone, String initialPassword,
                                        String newPassword, String driverIdNo, String driverLicenseNo)
            throws Exception {
        long vehicleId = data(postJson("/api/logistics/vehicles", fixture.token,
                vehicleBody(plateNo, vehicleLicenseNo))).path("vehicleId").asLong();
        JsonNode driver = data(postJson("/api/logistics/drivers", fixture.token,
                driverBody(driverName, driverPhone, initialPassword, driverIdNo, driverLicenseNo, vehicleId)));
        long driverId = driver.path("driverId").asLong();
        long driverAccountId = driver.path("accountId").asLong();

        JsonNode firstLogin = loginBody(driverPhone, initialPassword);
        assertTrue(firstLogin.path("mustChangePassword").asBoolean());
        String firstToken = firstLogin.path("token").asText();
        postJson("/api/auth/password", firstToken,
                Map.of("oldPassword", initialPassword, "newPassword", newPassword))
                .andExpect(status().isOk());
        getJson("/api/auth/profile", firstToken).andExpect(status().isUnauthorized());
        JsonNode relogin = loginBody(driverPhone, newPassword);
        assertFalse(relogin.path("mustChangePassword").asBoolean());

        fixture.vehicleId = vehicleId;
        fixture.driverId = driverId;
        fixture.driverAccountId = driverAccountId;
        fixture.driverToken = relogin.path("token").asText();
    }

    private void register(String role, String phone, String name, String password) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("role", role);
        body.put("phone", phone);
        body.put("name", name);
        body.put("email", phone + "@example.com");
        body.put("password", password);
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(body)))
                .andExpect(status().isOk());
    }

    private void submitRealname(String token, String name, String idNo, String phone) throws Exception {
        JsonNode result = data(postJson("/api/realname/submit", token,
                Map.of("realName", name, "idType", "ID_CARD", "idNo", idNo, "phone", phone)));
        assertEquals("PASSED", result.path("verifyStatus").asText());
    }

    private long submitOnboarding(String token, String subjectName, String creditCode, String legalRepName,
                                  String legalRepIdNo, String contactPhone) throws Exception {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subjectName", subjectName);
        body.put("creditCode", creditCode);
        body.put("legalRepName", legalRepName);
        body.put("legalRepIdType", "ID_CARD");
        body.put("legalRepIdNo", legalRepIdNo);
        body.put("contactName", "验收联系人");
        body.put("contactPhone", contactPhone);
        body.put("province", "北京市");
        body.put("city", "北京市");
        body.put("district", "朝阳区");
        body.put("businessScope", "普通货运");
        body.put("attachmentPaths", List.of());
        JsonNode result = data(postJson("/api/onboarding/submit", token, body));
        assertEquals("PENDING", result.path("auditStatus").asText());
        return result.path("id").asLong();
    }

    private void approveOnboarding(String adminToken, long onboardingId) throws Exception {
        postJson("/admin/onboardings/" + onboardingId + "/approve", adminToken, Map.of())
                .andExpect(status().isOk());
    }

    private long saveOrderDraft(String shipperToken, int goodsRows) throws Exception {
        JsonNode saved = data(postJson("/api/shipper/orders/save", shipperToken, orderBody(goodsRows)));
        assertEquals("DRAFT", saved.path("orderStatus").asText());
        return saved.path("id").asLong();
    }

    private Map<String, Object> orderBody(int goodsRows) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("freightAmount", 12000.0);
        body.put("validHours", 240);
        body.put("pickupTime", PICKUP_TIME);
        body.put("deliveryTime", DELIVERY_TIME);
        body.put("remark", "验收发货备注");
        List<Map<String, Object>> goods = new ArrayList<>();
        for (int index = 1; index <= goodsRows; index++) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("goodsName", "验收货物" + index);
            item.put("goodsType", "STEEL");
            item.put("quantity", 10 + index);
            item.put("weight", 20);
            item.put("volume", 8);
            item.put("unit", "件");
            goods.add(item);
        }
        body.put("goods", goods);
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

    private Map<String, Object> vehicleBody(String plateNo, String licenseNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("plateNo", plateNo);
        body.put("vehicleType", "HIGH_SIDE");
        body.put("licenseNo", licenseNo);
        body.put("licenseExpireDate", "2031-12-31");
        body.put("attachmentPaths", List.of());
        return body;
    }

    private Map<String, Object> driverBody(String name, String phone, String password, String idNo,
                                           String licenseNo, long vehicleId) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("name", name);
        body.put("phone", phone);
        body.put("password", password);
        body.put("idType", "ID_CARD");
        body.put("idNo", idNo);
        body.put("licenseNo", licenseNo);
        body.put("licenseExpireDate", "2031-12-31");
        body.put("email", phone + "@example.com");
        body.put("vehicleId", vehicleId);
        return body;
    }

    private Map<String, Object> reimbursementBody(long taskId, Double amount, String expenseType,
                                                  String occurredDate, String receipt) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("taskId", taskId);
        body.put("amount", amount);
        body.put("expenseType", expenseType);
        body.put("occurredDate", occurredDate);
        body.put("description", "验收现场费用");
        body.put("photoPaths", List.of(receipt));
        return body;
    }

    private JsonNode loginBody(String phone, String password) throws Exception {
        MvcResult result = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of("username", phone, "password", password))))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(new String(result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8)).path("data");
    }

    private String[] raceAccept(String path, String firstToken, String secondToken) throws Exception {
        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            CountDownLatch ready = new CountDownLatch(2);
            CountDownLatch go = new CountDownLatch(1);
            Callable<String> first = () -> acceptConcurrently(path, firstToken, ready, go);
            Callable<String> second = () -> acceptConcurrently(path, secondToken, ready, go);
            Future<String> firstFuture = executor.submit(first);
            Future<String> secondFuture = executor.submit(second);
            assertTrue(ready.await(5, TimeUnit.SECONDS));
            go.countDown();
            return new String[]{firstFuture.get(10, TimeUnit.SECONDS),
                    secondFuture.get(10, TimeUnit.SECONDS)};
        } finally {
            executor.shutdownNow();
        }
    }

    private String acceptConcurrently(String path, String token, CountDownLatch ready, CountDownLatch go)
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

    private void assertTaskCheckinVisible(JsonNode task) {
        assertEquals(1, task.path("departCheckins").size());
        assertEquals(1, task.path("arriveCheckins").size());
        JsonNode depart = task.path("departCheckins").get(0);
        assertTrue(containsPurpose(depart.path("photos"), "LOAD_DONE"));
        assertTrue(containsPurpose(depart.path("photos"), "VEHICLE_STATE"));
        JsonNode arrive = task.path("arriveCheckins").get(0);
        assertTrue(containsPurpose(arrive.path("photos"), "UNLOAD_DONE"));
        assertTrue(containsPurpose(arrive.path("photos"), "SIGN_RECEIPT"));
        assertFalse(depart.path("submitTime").asText().isBlank());
    }

    private boolean containsPurpose(JsonNode photos, String purpose) {
        for (JsonNode photo : photos) {
            if (purpose.equals(photo.path("purpose").asText())) {
                return true;
            }
        }
        return false;
    }

    /**
     * 物流公司验收夹具，保存账号、车辆、司机与令牌。
     */
    private static final class LogisticsFixture {

        private final String token;
        private final long accountId;
        private long vehicleId;
        private long driverId;
        private long driverAccountId;
        private String driverToken;

        private LogisticsFixture(String token, long accountId) {
            this.token = token;
            this.accountId = accountId;
        }
    }
}
