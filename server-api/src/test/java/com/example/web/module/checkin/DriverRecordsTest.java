package com.example.web.module.checkin;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 票据 15：司机 Web 全部现场记录查询的聚合、筛选、分页与数据隔离 HTTP 接口测试。
 */
class DriverRecordsTest extends FreightApiTestBase {

    @Test
    void recordsAggregateVersionsPhotosRemarksAndFilters() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900009001");
        seedApprovedShipper("13900009002");
        String shipperToken = login("13900009002", DEFAULT_PASSWORD);
        String logisticsToken = login("13900009001", DEFAULT_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A90001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900009003");
        String driverToken = login("13900009003", DEFAULT_PASSWORD);
        Long driverId = driverProfileId(driverAccountId);
        String loadPhoto = seedFile(driverAccountId, "records-load.jpg");
        String vehiclePhoto = seedFile(driverAccountId, "records-vehicle.jpg");
        String loadPhotoTwo = seedFile(driverAccountId, "records-load-2.jpg");
        String vehiclePhotoTwo = seedFile(driverAccountId, "records-vehicle-2.jpg");
        String unloadPhoto = seedFile(driverAccountId, "records-unload.jpg");
        String signPhoto = seedFile(driverAccountId, "records-sign.jpg");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverId, vehicleId);

        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto)))));
        clock.advanceMinutes(10);
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.5, 39.95, List.of(photo("LOAD_DONE", loadPhotoTwo),
                        photo("VEHICLE_STATE", vehiclePhotoTwo)))));
        clock.setNow(LocalDateTime.of(2026, 12, 9, 10, 0));
        data(postJson("/api/driver/tasks/" + taskId + "/checkin/arrive", driverToken,
                checkinBody(113.3, 23.1, List.of(photo("UNLOAD_DONE", unloadPhoto),
                        photo("SIGN_RECEIPT", signPhoto)))));
        postJson("/api/driver/tasks/" + taskId + "/checkin/remark", driverToken,
                Map.of("node", "ARRIVE", "content", "收货方已签字确认"))
                .andExpect(status().isOk());

        String waybillNo = data(getJson("/api/shipper/orders/" + orderId, shipperToken)).path("waybillNo").asText();
        JsonNode page = data(getJson("/api/driver/records", driverToken, Map.of("pageSize", "10")));
        assertEquals(3, page.path("total").asLong());
        assertEquals(1, page.path("pageNum").asInt());
        assertEquals(10, page.path("pageSize").asInt());

        JsonNode arrive = page.path("records").get(0);
        assertEquals(taskId, arrive.path("taskId").asLong());
        assertFalse(arrive.path("taskNo").asText().isBlank());
        assertEquals(waybillNo, arrive.path("waybillNo").asText());
        assertEquals(orderId, arrive.path("orderId").asLong());
        assertEquals("ARRIVE", arrive.path("node").asText());
        assertEquals(1, arrive.path("versionNo").asInt());
        assertEquals(1, arrive.path("isValid").asInt());
        assertEquals(1, arrive.path("onTime").asInt());
        assertEquals("2026-12-09 10:00:00", arrive.path("submitTime").asText());
        assertEquals(113.3, arrive.path("longitude").asDouble(), 0.001);
        assertEquals(23.1, arrive.path("latitude").asDouble(), 0.001);
        assertEquals("现场地址", arrive.path("addressText").asText());
        assertEquals(2, arrive.path("photos").size());
        assertTrue(containsPurpose(arrive.path("photos"), "UNLOAD_DONE"));
        assertTrue(containsPurpose(arrive.path("photos"), "SIGN_RECEIPT"));
        assertEquals(1, arrive.path("remarks").size());
        assertEquals("收货方已签字确认", arrive.path("remarks").get(0).path("content").asText());

        JsonNode latestDepart = page.path("records").get(1);
        assertEquals("DEPART", latestDepart.path("node").asText());
        assertEquals(2, latestDepart.path("versionNo").asInt());
        assertEquals(1, latestDepart.path("isValid").asInt());
        assertEquals("2026-12-08 08:10:00", latestDepart.path("submitTime").asText());
        assertEquals(2, latestDepart.path("photos").size());

        JsonNode firstDepart = page.path("records").get(2);
        assertEquals(1, firstDepart.path("versionNo").asInt());
        assertEquals(0, firstDepart.path("isValid").asInt());
        assertEquals("2026-12-08 08:00:00", firstDepart.path("submitTime").asText());

        assertEquals(2, data(getJson("/api/driver/records", driverToken,
                Map.of("node", "DEPART", "pageSize", "10"))).path("total").asLong());
        assertEquals(1, data(getJson("/api/driver/records", driverToken,
                Map.of("node", "ARRIVE", "pageSize", "10"))).path("total").asLong());
        assertEquals(1, data(getJson("/api/driver/records", driverToken,
                Map.of("startTime", "2026-12-09 00:00:00", "pageSize", "10"))).path("total").asLong());
        assertEquals(2, data(getJson("/api/driver/records", driverToken,
                Map.of("endTime", "2026-12-08 23:59:59", "pageSize", "10"))).path("total").asLong());

        JsonNode firstPage = data(getJson("/api/driver/records", driverToken,
                Map.of("pageNum", "1", "pageSize", "2")));
        assertEquals(3, firstPage.path("total").asLong());
        assertEquals(2, firstPage.path("records").size());
        JsonNode secondPage = data(getJson("/api/driver/records", driverToken,
                Map.of("pageNum", "2", "pageSize", "2")));
        assertEquals(3, secondPage.path("total").asLong());
        assertEquals(1, secondPage.path("records").size());
        assertEquals(taskId, secondPage.path("records").get(0).path("taskId").asLong());

        assertEquals(3, data(getJson("/api/driver/records", driverToken,
                Map.of("taskId", String.valueOf(taskId), "pageSize", "10"))).path("total").asLong());
    }

    @Test
    void recordsRejectForeignTaskBadNodeAndNonDriver() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900009011");
        seedApprovedShipper("13900009012");
        String shipperToken = login("13900009012", DEFAULT_PASSWORD);
        String logisticsToken = login("13900009011", DEFAULT_PASSWORD);

        Long firstVehicle = seedVehicle(logisticsAccountId, "京A90011");
        Long secondVehicle = seedVehicle(logisticsAccountId, "京A90012");
        Long firstDriverAccountId = seedDriver(logisticsAccountId, firstVehicle, "13900009013");
        Long secondDriverAccountId = seedDriver(logisticsAccountId, secondVehicle, "13900009014");
        String firstDriverToken = login("13900009013", DEFAULT_PASSWORD);
        String secondDriverToken = login("13900009014", DEFAULT_PASSWORD);
        String firstLoad = seedFile(firstDriverAccountId, "records-foreign-load.jpg");
        String firstVehiclePhoto = seedFile(firstDriverAccountId, "records-foreign-vehicle.jpg");
        String secondLoad = seedFile(secondDriverAccountId, "records-other-load.jpg");
        String secondVehiclePhoto = seedFile(secondDriverAccountId, "records-other-vehicle.jpg");

        long orderId = publishOrder(shipperToken, 2);
        acceptOrder(logisticsToken, orderId);
        List<Long> goodsIds = listGoodsIds(logisticsToken, orderId);
        long firstTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(0))).path("taskId").asLong();
        long secondTask = splitTask(logisticsToken, orderId, List.of(goodsIds.get(1))).path("taskId").asLong();
        dispatchTask(logisticsToken, firstTask, driverProfileId(firstDriverAccountId), firstVehicle);
        dispatchTask(logisticsToken, secondTask, driverProfileId(secondDriverAccountId), secondVehicle);

        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        postJson("/api/driver/tasks/" + firstTask + "/checkin/depart", firstDriverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", firstLoad),
                        photo("VEHICLE_STATE", firstVehiclePhoto))))
                .andExpect(status().isOk());
        postJson("/api/driver/tasks/" + secondTask + "/checkin/depart", secondDriverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", secondLoad),
                        photo("VEHICLE_STATE", secondVehiclePhoto))))
                .andExpect(status().isOk());

        getJson("/api/driver/records", firstDriverToken, Map.of("taskId", String.valueOf(secondTask)))
                .andExpect(status().isForbidden());
        getJson("/api/driver/records", secondDriverToken, Map.of("taskId", String.valueOf(firstTask)))
                .andExpect(status().isForbidden());
        getJson("/api/driver/records", firstDriverToken, Map.of("node", "UNKNOWN"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("打卡节点不正确")));
        getJson("/api/driver/records", firstDriverToken, Map.of("taskId", "999999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("任务不存在")));
        getJson("/api/driver/records", shipperToken, Map.of())
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("没有权限访问")));
        mockMvc.perform(get("/api/driver/records")).andExpect(status().isUnauthorized());

        assertEquals(1, data(getJson("/api/driver/records", firstDriverToken,
                Map.of("taskId", String.valueOf(firstTask), "pageSize", "10"))).path("total").asLong());
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
