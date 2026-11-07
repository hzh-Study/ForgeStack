package com.example.web.module.realname;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

/**
 * 票据 02：实名自动校验、唯一身份与证件号脱敏的 HTTP 接口测试。
 */
class RealnameVerificationTest extends ApiTestBase {

    private static final String VALID_ID_NO = "110101199001011234";
    private static final String FAILED_ID_NO = "110101199001019999";

    @Test
    void realnamePassedCannotBeResubmittedAndIsMaskedInAdminList() throws Exception {
        String token = registerAndLogin("SHIPPER", "13900000001", "货主实名", "shipper123");

        JsonNode submitted = data(postJson("/api/realname/submit", token, realnameBody("张三", VALID_ID_NO)));
        assertEquals("PASSED", submitted.path("verifyStatus").asText());
        assertEquals(VALID_ID_NO, submitted.path("idNo").asText());
        assertTrue(submitted.hasNonNull("verifyTime"));

        JsonNode mine = data(getJson("/api/realname", token));
        assertEquals(VALID_ID_NO, mine.path("idNo").asText());
        assertEquals("PERSONAL", mine.path("realnameType").asText());

        postJson("/api/realname/submit", token, realnameBody("张三", VALID_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("已通过")));

        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        JsonNode page = data(getJson("/admin/realnames", adminToken, Map.of("pageSize", "10")));
        assertEquals(1, page.path("total").asLong());
        assertEquals("110****1234", page.path("records").get(0).path("idNo").asText());
        assertFalse(page.toString().contains(VALID_ID_NO));
        assertEquals("PASSED", page.path("records").get(0).path("verifyStatus").asText());

        JsonNode profile = data(getJson("/api/auth/profile", token));
        assertEquals("PASSED", profile.path("realnameStatus").asText());
    }

    @Test
    void realnameFailedReturnsReasonAndCanRetry() throws Exception {
        String token = registerAndLogin("LOGISTICS", "13900000002", "物流实名", "logistics123");

        JsonNode submitted = data(postJson("/api/realname/submit", token, realnameBody("李四", FAILED_ID_NO)));
        assertEquals("FAILED", submitted.path("verifyStatus").asText());
        assertEquals("模拟校验：证件信息不匹配", submitted.path("failReason").asText());

        JsonNode profile = data(getJson("/api/auth/profile", token));
        assertEquals("FAILED", profile.path("realnameStatus").asText());
        assertEquals("模拟校验：证件信息不匹配", profile.path("realnameFailReason").asText());

        JsonNode retried = data(postJson("/api/realname/submit", token, realnameBody("李四", VALID_ID_NO)));
        assertEquals("PASSED", retried.path("verifyStatus").asText());

        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        JsonNode page = data(getJson("/admin/realnames", adminToken, Map.of("pageSize", "10")));
        assertEquals(1, page.path("total").asLong());
    }

    @Test
    void sameIdentityCannotBindTwoAccounts() throws Exception {
        String firstToken = registerAndLogin("SHIPPER", "13900000003", "货主甲", "shipper123");
        String secondToken = registerAndLogin("LOGISTICS", "13900000004", "物流乙", "logistics123");

        data(postJson("/api/realname/submit", firstToken, realnameBody("王五", VALID_ID_NO)));

        postJson("/api/realname/submit", secondToken, realnameBody("王五", VALID_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("该实名身份已绑定其他账号")));

        JsonNode mine = data(getJson("/api/realname", secondToken));
        assertTrue(mine.isNull());
    }

    @Test
    void adminRealnameListFiltersByRoleAndStatus() throws Exception {
        String shipperToken = registerAndLogin("SHIPPER", "13900000005", "货主筛选", "shipper123");
        data(postJson("/api/realname/submit", shipperToken, realnameBody("赵六", VALID_ID_NO)));

        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        assertEquals(1, data(getJson("/admin/realnames", adminToken,
                Map.of("role", "SHIPPER", "verifyStatus", "PASSED"))).path("total").asLong());
        assertEquals(0, data(getJson("/admin/realnames", adminToken,
                Map.of("role", "LOGISTICS"))).path("total").asLong());
        assertEquals(0, data(getJson("/admin/realnames", adminToken,
                Map.of("verifyStatus", "FAILED"))).path("total").asLong());
    }

    private Map<String, String> realnameBody(String realName, String idNo) {
        return Map.of("realName", realName, "idType", "ID_CARD", "idNo", idNo, "phone", "13900000001");
    }

    private String registerAndLogin(String role, String phone, String name, String password) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", role, "phone", phone, "name", name,
                                "email", phone + "@example.com", "password", password))))
                .andExpect(status().isOk());
        return login(phone, password);
    }
}
