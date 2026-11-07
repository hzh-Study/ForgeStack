package com.example.web.module.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.logistics.entity.Logistics;
import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.nio.charset.StandardCharsets;
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
 * 票据 07：订单池筛选排序、公开投影与并发接单唯一性的 HTTP 接口测试。
 */
class OrderPoolTest extends ApiTestBase {

    @Test
    void poolSupportsFiltersSortsAndHidesPrivateFields() throws Exception {
        seedApprovedShipper("13900000201");
        seedApprovedLogistics("13900000202");
        String shipperToken = login("13900000201", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000202", DEFAULT_PASSWORD);

        publishOrder(shipperToken, 10000.0, "北京市", "北京市", "广东省", "广州市", "STEEL", "钢材");
        publishOrder(shipperToken, 20000.0, "上海市", "上海市", "广东省", "广州市", "COAL", "煤炭");
        publishOrder(shipperToken, 15000.0, "北京市", "北京市", "广东省", "深圳市", "DAILY", "日用品");

        JsonNode all = data(getJson("/api/logistics/order-pool", logisticsToken, Map.of("pageSize", "10")));
        assertEquals(3, all.path("total").asLong());

        JsonNode byPickupProvince = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("pickupProvince", "北京市", "pageSize", "10")));
        assertEquals(2, byPickupProvince.path("total").asLong());

        JsonNode byProvinceAndGoods = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("pickupProvince", "北京市", "goodsType", "STEEL", "pageSize", "10")));
        assertEquals(1, byProvinceAndGoods.path("total").asLong());
        assertEquals("钢材", byProvinceAndGoods.path("records").get(0).path("goods").get(0)
                .path("goodsName").asText());

        JsonNode byDeliveryCityAndGoods = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("deliveryCity", "广州市", "goodsType", "DAILY", "pageSize", "10")));
        assertEquals(0, byDeliveryCityAndGoods.path("total").asLong());

        JsonNode byFreightRange = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("minFreight", "15000", "maxFreight", "20000", "pageSize", "10")));
        assertEquals(2, byFreightRange.path("total").asLong());

        JsonNode sorted = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("sortBy", "freightAmount", "sortOrder", "desc", "pageSize", "10")));
        assertEquals(20000.0, sorted.path("records").get(0).path("freightAmount").asDouble());

        JsonNode sortedAsc = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("sortBy", "freightAmount", "sortOrder", "asc", "pageSize", "10")));
        assertEquals(10000.0, sortedAsc.path("records").get(0).path("freightAmount").asDouble());

        String poolJson = all.toString();
        assertFalse(poolJson.contains("contactName"));
        assertFalse(poolJson.contains("contactPhone"));
        assertFalse(poolJson.contains("\"detail\""));
        assertFalse(poolJson.contains("remark"));
    }

    @Test
    void concurrentAcceptOnlyOneLogisticsWins() throws Exception {
        seedApprovedShipper("13900000203");
        seedApprovedLogistics("13900000204");
        seedApprovedLogistics("13900000205");
        String shipperToken = login("13900000203", DEFAULT_PASSWORD);
        String firstToken = login("13900000204", DEFAULT_PASSWORD);
        String secondToken = login("13900000205", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 12000.0, "北京市", "北京市", "广东省", "广州市", "STEEL", "钢材");
        String acceptPath = "/api/logistics/order-pool/" + orderId + "/accept";

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch ready = new CountDownLatch(2);
        CountDownLatch go = new CountDownLatch(1);
        Callable<String> first = () -> acceptConcurrently(acceptPath, firstToken, ready, go);
        Callable<String> second = () -> acceptConcurrently(acceptPath, secondToken, ready, go);
        Future<String> firstFuture = executor.submit(first);
        Future<String> secondFuture = executor.submit(second);
        assertTrue(ready.await(5, TimeUnit.SECONDS));
        go.countDown();
        String firstResult = firstFuture.get(10, TimeUnit.SECONDS);
        String secondResult = secondFuture.get(10, TimeUnit.SECONDS);
        executor.shutdownNow();

        int firstStatus = Integer.parseInt(firstResult.substring(0, firstResult.indexOf('|')));
        int secondStatus = Integer.parseInt(secondResult.substring(0, secondResult.indexOf('|')));
        int successCount = (firstStatus == 200 ? 1 : 0) + (secondStatus == 200 ? 1 : 0);
        int conflictCount = (firstStatus == 400 ? 1 : 0) + (secondStatus == 400 ? 1 : 0);
        assertEquals(1, successCount, "并发接单必须只有一家成功");
        assertEquals(1, conflictCount, "另一家必须收到冲突提示");
        String conflictBody = firstStatus == 400 ? firstResult : secondResult;
        assertTrue(conflictBody.contains("已被接走"));

        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("ACCEPTED", detail.path("orderStatus").asText());
        assertTrue(detail.path("waybillNo").asText().length() > 0);
        assertFalse(detail.path("logisticsAccountId").isNull());
        assertFalse(detail.path("logisticsName").asText().isEmpty());
        assertFalse(detail.path("logisticsContactName").asText().isEmpty());
        assertFalse(detail.path("logisticsContactPhone").asText().isEmpty());

        assertEquals(0, data(getJson("/api/logistics/order-pool", firstToken, Map.of("pageSize", "10"))
                ).path("total").asLong());
    }

    @Test
    void withdrawnOrderCannotBeAcceptedOrViewedInPool() throws Exception {
        seedApprovedShipper("13900000206");
        seedApprovedLogistics("13900000207");
        String shipperToken = login("13900000206", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000207", DEFAULT_PASSWORD);

        long orderId = publishOrder(shipperToken, 12000.0, "北京市", "北京市", "广东省", "广州市", "STEEL", "钢材");
        postJson("/api/shipper/orders/" + orderId + "/withdraw", shipperToken, Map.of())
                .andExpect(status().isOk());

        postJson("/api/logistics/order-pool/" + orderId + "/accept", logisticsToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.message", org.hamcrest.Matchers.containsString("订单当前状态不可接单")));
        getJson("/api/logistics/order-pool/" + orderId, logisticsToken)
                .andExpect(status().isBadRequest());
    }

    @Test
    void orderPoolRequiresApprovedLogistics() throws Exception {
        var account = createAccount(AccountRoleEnum.LOGISTICS, "13900000208", DEFAULT_PASSWORD);
        Logistics logistics = new Logistics();
        logistics.setAccountId(account.getId());
        logistics.setName("物流13900000208");
        logistics.setPhone("13900000208");
        logisticsMapper.insert(logistics);
        String token = login("13900000208", DEFAULT_PASSWORD);

        getJson("/api/logistics/order-pool", token, Map.of("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.message", org.hamcrest.Matchers.containsString("请先完成企业入驻审核")));
    }

    @Test
    void shipperCannotUseLogisticsOrderPool() throws Exception {
        seedApprovedShipper("13900000209");
        String token = login("13900000209", DEFAULT_PASSWORD);
        getJson("/api/logistics/order-pool", token, Map.of("pageSize", "10"))
                .andExpect(status().isBadRequest())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers
                        .jsonPath("$.message", org.hamcrest.Matchers.containsString("无权访问当前业务")));
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

    private long publishOrder(String shipperToken, double freight, String pickupProvince, String pickupCity,
                              String deliveryProvince, String deliveryCity, String goodsType,
                              String goodsName) throws Exception {
        Map<String, Object> body = orderBody(freight, pickupProvince, pickupCity, deliveryProvince, deliveryCity,
                goodsType, goodsName);
        long orderId = data(postJson("/api/shipper/orders/save", shipperToken, body)).path("id").asLong();
        postJson("/api/shipper/orders/" + orderId + "/publish", shipperToken, Map.of())
                .andExpect(status().isOk());
        return orderId;
    }

    private Map<String, Object> orderBody(double freightAmount, String pickupProvince, String pickupCity,
                                          String deliveryProvince, String deliveryCity, String goodsType,
                                          String goodsName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("freightAmount", freightAmount);
        body.put("validHours", 240);
        body.put("pickupTime", "2026-12-08 08:00:00");
        body.put("deliveryTime", "2026-12-09 18:00:00");
        body.put("remark", "发货备注");
        Map<String, Object> goods = new LinkedHashMap<>();
        goods.put("goodsName", goodsName);
        goods.put("goodsType", goodsType);
        goods.put("quantity", 20);
        goods.put("weight", 30);
        goods.put("volume", 12);
        goods.put("unit", "件");
        body.put("goods", List.of(goods));
        body.put("pickup", address(pickupProvince, pickupCity, "朝阳区", "装货详细地址", "张三", "13900000001"));
        body.put("delivery", address(deliveryProvince, deliveryCity, "天河区", "卸货详细地址", "收货人",
                "13700000000"));
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
