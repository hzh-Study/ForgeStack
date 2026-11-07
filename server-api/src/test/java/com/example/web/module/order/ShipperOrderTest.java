package com.example.web.module.order;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 06：货主发货草稿、发布、我的订单与隐私投影的 HTTP 接口测试。
 */
class ShipperOrderTest extends ApiTestBase {

    @Test
    void draftStaysOutOfPoolAndCanBeEditedAndPublished() throws Exception {
        seedApprovedShipper("13900000101");
        seedApprovedLogistics("13900000102");
        String shipperToken = login("13900000101", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000102", DEFAULT_PASSWORD);

        JsonNode created = data(postJson("/api/shipper/orders/save", shipperToken,
                orderBody(12000.0, 240, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材")));
        long orderId = created.path("id").asLong();
        assertEquals("DRAFT", created.path("orderStatus").asText());

        JsonNode drafts = data(getJson("/api/shipper/orders", shipperToken, Map.of("orderStatus", "DRAFT")));
        assertEquals(1, drafts.path("total").asLong());
        assertEquals("DRAFT", drafts.path("records").get(0).path("orderStatus").asText());

        JsonNode poolBeforePublish = data(getJson("/api/logistics/order-pool", logisticsToken,
                Map.of("pageSize", "10")));
        assertEquals(0, poolBeforePublish.path("total").asLong());

        Map<String, Object> edited = orderBody(15000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        edited.put("id", orderId);
        JsonNode updated = data(postJson("/api/shipper/orders/save", shipperToken, edited));
        assertEquals(orderId, updated.path("id").asLong());
        assertEquals("DRAFT", updated.path("orderStatus").asText());

        JsonNode draftDetail = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals(15000.0, draftDetail.path("freightAmount").asDouble());
        assertEquals(1, draftDetail.path("goods").size());
        assertEquals("钢材", draftDetail.path("goods").get(0).path("goodsName").asText());
        assertEquals("张三", draftDetail.path("pickup").path("contactName").asText());
        assertEquals("装货详细地址", draftDetail.path("pickup").path("detail").asText());

        postJson("/api/shipper/orders/" + orderId + "/publish", shipperToken, Map.of())
                .andExpect(status().isOk());

        JsonNode published = data(getJson("/api/shipper/orders/" + orderId, shipperToken));
        assertEquals("PENDING_ACCEPT", published.path("orderStatus").asText());
        assertTrue(published.path("orderNo").asText().length() > 0);
        assertTrue(published.hasNonNull("publishTime"));
        assertTrue(published.hasNonNull("expireTime"));
        assertEquals(240, published.path("validHours").asInt());

        JsonNode pool = data(getJson("/api/logistics/order-pool", logisticsToken, Map.of("pageSize", "10")));
        assertEquals(1, pool.path("total").asLong());
        JsonNode poolRecord = pool.path("records").get(0);
        assertEquals(orderId, poolRecord.path("id").asLong());
        assertEquals("北京市", poolRecord.path("pickupProvince").asText());
        assertEquals("广东省", poolRecord.path("deliveryProvince").asText());
        assertTrue(poolRecord.path("remainingSeconds").asLong() > 0);
        assertTrue(poolRecord.path("goods").size() > 0);
        String poolJson = pool.toString();
        assertFalse(poolJson.contains("contactName"));
        assertFalse(poolJson.contains("contactPhone"));
        assertFalse(poolJson.contains("\"detail\""));
        assertFalse(poolJson.contains("remark"));

        JsonNode poolDetail = data(getJson("/api/logistics/order-pool/" + orderId, logisticsToken));
        assertEquals(orderId, poolDetail.path("id").asLong());
        assertEquals("北京市", poolDetail.path("pickupProvince").asText());
        String poolDetailJson = poolDetail.toString();
        assertFalse(poolDetailJson.contains("contactName"));
        assertFalse(poolDetailJson.contains("contactPhone"));
        assertFalse(poolDetailJson.contains("\"detail\""));
        assertFalse(poolDetailJson.contains("remark"));
    }

    @Test
    void validHoursMustStayWithinRangeAndDefaultToPlatformParam() throws Exception {
        seedApprovedShipper("13900000103");
        String token = login("13900000103", DEFAULT_PASSWORD);

        postJson("/api/shipper/orders/save", token,
                orderBody(12000.0, 12, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("有效期")));
        postJson("/api/shipper/orders/save", token,
                orderBody(12000.0, 800, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("有效期")));

        Map<String, Object> defaultBody = orderBody(12000.0, null, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        long orderId = data(postJson("/api/shipper/orders/save", token, defaultBody)).path("id").asLong();
        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, token));
        assertEquals(240, detail.path("validHours").asInt());
    }

    @Test
    void invalidGoodsAndAddressAreRejected() throws Exception {
        seedApprovedShipper("13900000104");
        String token = login("13900000104", DEFAULT_PASSWORD);

        Map<String, Object> unknownGoodsType = orderBody(12000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "UNKNOWN", "钢材");
        postJson("/api/shipper/orders/save", token, unknownGoodsType)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("货物类型")));

        Map<String, Object> zeroWeight = orderBody(12000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        @SuppressWarnings("unchecked")
        Map<String, Object> goods = ((List<Map<String, Object>>) zeroWeight.get("goods")).get(0);
        goods.put("weight", 0);
        postJson("/api/shipper/orders/save", token, zeroWeight)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("重量")));

        Map<String, Object> deliveryBeforePickup = orderBody(12000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        deliveryBeforePickup.put("deliveryTime", "2026-12-07 18:00:00");
        postJson("/api/shipper/orders/save", token, deliveryBeforePickup)
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("送达时间")));

        Map<String, Object> missingAddress = orderBody(12000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        missingAddress.remove("pickup");
        postJson("/api/shipper/orders/save", token, missingAddress)
                .andExpect(status().isBadRequest());
    }

    @Test
    void publishRequiresApprovedShipper() throws Exception {
        seedShipper("13900000105");
        seedApprovedLogistics("13900000106");
        String token = login("13900000105", DEFAULT_PASSWORD);
        String logisticsToken = login("13900000106", DEFAULT_PASSWORD);

        long orderId = data(postJson("/api/shipper/orders/save", token,
                orderBody(12000.0, 240, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材")))
                .path("id").asLong();

        postJson("/api/shipper/orders/" + orderId + "/publish", token, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("请先完成企业入驻审核")));

        JsonNode detail = data(getJson("/api/shipper/orders/" + orderId, token));
        assertEquals("DRAFT", detail.path("orderStatus").asText());
        assertEquals(0, data(getJson("/api/logistics/order-pool", logisticsToken, Map.of("pageSize", "10"))
                ).path("total").asLong());
    }

    @Test
    void shipperCannotReadOtherShippersOrders() throws Exception {
        seedApprovedShipper("13900000107");
        seedApprovedShipper("13900000108");
        String tokenA = login("13900000107", DEFAULT_PASSWORD);
        String tokenB = login("13900000108", DEFAULT_PASSWORD);

        long orderId = data(postJson("/api/shipper/orders/save", tokenA,
                orderBody(12000.0, 240, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材")))
                .path("id").asLong();

        getJson("/api/shipper/orders/" + orderId, tokenB)
                .andExpect(status().isForbidden());
        JsonNode mine = data(getJson("/api/shipper/orders", tokenB, Map.of("pageSize", "10")));
        assertEquals(0, mine.path("total").asLong());

        Map<String, Object> hijack = orderBody(8000.0, 240, "北京市", "北京市", "朝阳区",
                "广东省", "广州市", "天河区", "STEEL", "钢材");
        hijack.put("id", orderId);
        postJson("/api/shipper/orders/save", tokenB, hijack).andExpect(status().isForbidden());
    }

    @Test
    void myOrdersSupportStatusAndPublishTimeFilters() throws Exception {
        seedApprovedShipper("13900000109");
        String token = login("13900000109", DEFAULT_PASSWORD);

        long orderId = data(postJson("/api/shipper/orders/save", token,
                orderBody(12000.0, 240, "北京市", "北京市", "朝阳区", "广东省", "广州市", "天河区", "STEEL", "钢材")))
                .path("id").asLong();
        postJson("/api/shipper/orders/" + orderId + "/publish", token, Map.of())
                .andExpect(status().isOk());

        JsonNode pending = data(getJson("/api/shipper/orders", token, Map.of("orderStatus", "PENDING_ACCEPT")));
        assertEquals(1, pending.path("total").asLong());

        JsonNode future = data(getJson("/api/shipper/orders", token,
                Map.of("startTime", "2030-01-01 00:00:00", "endTime", "2030-12-31 23:59:59")));
        assertEquals(0, future.path("total").asLong());
    }

    private Map<String, Object> orderBody(Double freightAmount, Integer validHours, String pickupProvince,
                                           String pickupCity, String pickupDistrict, String deliveryProvince,
                                           String deliveryCity, String deliveryDistrict, String goodsType,
                                           String goodsName) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("freightAmount", freightAmount);
        if (validHours != null) {
            body.put("validHours", validHours);
        }
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
        goods.put("remark", "货物备注");
        body.put("goods", List.of(goods));
        body.put("pickup", address(pickupProvince, pickupCity, pickupDistrict, "装货详细地址", "张三", "13900000001"));
        body.put("delivery", address(deliveryProvince, deliveryCity, deliveryDistrict, "卸货详细地址", "收货人",
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
