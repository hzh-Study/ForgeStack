package com.example.web.module.file;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.FreightApiTestBase;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;

/**
 * 票据 79：文件上传与鉴权读取的角色范围、业务参与方与公开静态目录封禁 HTTP 接口测试。
 */
class FileAccessTest extends FreightApiTestBase {

    @Test
    void driverCheckinPhotoReadableByParticipantsOnly() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900007001");
        seedApprovedLogistics("13900007002");
        seedApprovedShipper("13900007003");
        String shipperToken = login("13900007003", DEFAULT_PASSWORD);
        String logisticsToken = login("13900007001", DEFAULT_PASSWORD);
        String otherLogisticsToken = login("13900007002", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        Long vehicleId = seedVehicle(logisticsAccountId, "京A70001");
        Long driverAccountId = seedDriver(logisticsAccountId, vehicleId, "13900007004");
        String driverToken = login("13900007004", DEFAULT_PASSWORD);
        String loadPhoto = upload(driverToken, "load.png", "image/png");
        String vehiclePhoto = upload(driverToken, "vehicle.png", "image/png");

        long orderId = publishOrder(shipperToken, 1);
        acceptOrder(logisticsToken, orderId);
        long taskId = splitTask(logisticsToken, orderId, listGoodsIds(logisticsToken, orderId))
                .path("taskId").asLong();
        dispatchTask(logisticsToken, taskId, driverProfileId(driverAccountId), vehicleId);
        clock.setNow(LocalDateTime.of(2026, 12, 8, 8, 0));
        postJson("/api/driver/tasks/" + taskId + "/checkin/depart", driverToken,
                checkinBody(116.4, 39.9, List.of(photo("LOAD_DONE", loadPhoto),
                        photo("VEHICLE_STATE", vehiclePhoto))))
                .andExpect(status().isOk());

        MvcResult ownView = view(loadPhoto, driverToken, 200);
        assertEquals("image/png", ownView.getResponse().getContentType());
        assertTrue(ownView.getResponse().getContentAsByteArray().length > 0);

        view(loadPhoto, logisticsToken, 200);
        view(loadPhoto, shipperToken, 200);
        view(loadPhoto, adminToken, 200);
        view(loadPhoto, otherLogisticsToken, 403);
        mockMvc.perform(get("/api/files/view").param("path", loadPhoto))
                .andExpect(status().isUnauthorized());
        view(vehiclePhoto, otherLogisticsToken, 403);
    }

    @Test
    void adminUploadedFileIsNotReadableByDriverAndStaticPathIsBlocked() throws Exception {
        Long logisticsAccountId = seedApprovedLogistics("13900007011");
        Long vehicleId = seedVehicle(logisticsAccountId, "京A70002");
        seedDriver(logisticsAccountId, vehicleId, "13900007013");
        String driverToken = login("13900007013", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        String adminFile = uploadAs("/admin/files/upload", adminToken, "admin.png", "image/png");
        view(adminFile, adminToken, 200);
        view(adminFile, driverToken, 403);
        mockMvc.perform(get("/api/files/view").param("path", adminFile))
                .andExpect(status().isUnauthorized());

        int staticStatus = mockMvc.perform(get(adminFile)).andReturn().getResponse().getStatus();
        assertTrue(staticStatus == 401 || staticStatus == 404,
                "公开静态路径 /files/** 不可直接访问，实际状态码：" + staticStatus);
    }

    /**
     * 以业务账号上传文件并返回相对路径。
     *
     * @param token 账号令牌
     * @param fileName 文件名
     * @param contentType 内容类型
     * @return 文件相对路径
     */
    private String upload(String token, String fileName, String contentType) throws Exception {
        return uploadAs("/api/files/upload", token, fileName, contentType);
    }

    /**
     * 以指定上传接口上传文件并返回相对路径。
     *
     * @param path 上传接口路径
     * @param token 账号令牌
     * @param fileName 文件名
     * @param contentType 内容类型
     * @return 文件相对路径
     */
    private String uploadAs(String path, String token, String fileName, String contentType) throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", fileName, contentType, imageBytes());
        MvcResult result = mockMvc.perform(multipart(path)
                        .file(file)
                        .param("module", "checkin")
                        .param("fileType", "image")
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andReturn();
        return objectMapper.readTree(new String(result.getResponse().getContentAsByteArray(),
                StandardCharsets.UTF_8)).path("data").get(0).path("url").asText();
    }

    /**
     * 读取文件并断言状态码，返回响应结果。
     *
     * @param filePath 文件相对路径
     * @param token 账号令牌
     * @param expectedStatus 期望状态码
     * @return 响应结果
     */
    private MvcResult view(String filePath, String token, int expectedStatus) throws Exception {
        return mockMvc.perform(get("/api/files/view")
                        .param("path", filePath)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().is(expectedStatus))
                .andReturn();
    }

    /**
     * 构造最小 PNG 文件内容。
     *
     * @return 文件字节
     */
    private byte[] imageBytes() {
        return new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A,
                0x00, 0x00, 0x00, 0x0D, 0x49, 0x48, 0x44, 0x52, 0x00, 0x00, 0x00, 0x01,
                0x00, 0x00, 0x00, 0x01, 0x08, 0x06, 0x00, 0x00, 0x00, 0x1F, 0x15, (byte) 0xC4,
                (byte) 0x89, 0x00, 0x00, 0x00, 0x0A, 0x49, 0x44, 0x41, 0x54, 0x78, (byte) 0x9C,
                0x63, 0x00, 0x01, 0x00, 0x00, 0x05, 0x00, 0x01, 0x0D, 0x0A, 0x2D, (byte) 0xB4,
                0x00, 0x00, 0x00, 0x00, 0x49, 0x45, 0x4E, 0x44, (byte) 0xAE, 0x42, 0x60, (byte) 0x82};
    }
}
