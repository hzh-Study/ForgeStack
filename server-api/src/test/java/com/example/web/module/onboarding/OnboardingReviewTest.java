package com.example.web.module.onboarding;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.web.exception.BusinessException;
import com.example.web.module.account.enums.AccountRoleEnum;
import com.example.web.module.onboarding.service.OnboardingService;
import com.example.web.support.ApiTestBase;
import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

/**
 * 票据 03：企业入驻提交、管理员审核与入驻资格校验的 HTTP 接口测试。
 */
class OnboardingReviewTest extends ApiTestBase {

    private static final String LEGAL_REP_ID_NO = "110101198001012345";

    @Autowired
    private OnboardingService onboardingService;

    @Test
    void onboardingRequiresPersonalRealnameAndBusinessRole() throws Exception {
        String shipperToken = registerAndLogin("SHIPPER", "13900000001", "未实名货主", "shipper123");
        postJson("/api/onboarding/submit", shipperToken, onboardingBody("北京货主公司", "9111010100001234X9",
                LEGAL_REP_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("请先完成实名认证")));

        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        postJson("/api/onboarding/submit", adminToken, onboardingBody("管理员公司", "911101010000123456",
                LEGAL_REP_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("当前角色不支持提交入驻申请")));
    }

    @Test
    void onboardingSubmitCreatesPendingApplicationWithFiles() throws Exception {
        Long shipperAccountId = seedShipper("13900000002");
        String token = login("13900000002", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);

        JsonNode submitted = data(postJson("/api/onboarding/submit", token,
                onboardingBody("北京顺达货主", "911101010000123456", LEGAL_REP_ID_NO)));
        long onboardingId = submitted.path("id").asLong();
        assertEquals("PENDING", submitted.path("auditStatus").asText());

        JsonNode mine = data(getJson("/api/onboarding", token));
        assertEquals(onboardingId, mine.path("id").asLong());
        assertEquals("PENDING", mine.path("auditStatus").asText());
        assertEquals("北京顺达货主", mine.path("subjectName").asText());
        assertEquals(LEGAL_REP_ID_NO, mine.path("legalRepIdNo").asText());
        assertEquals(2, mine.path("files").size());

        JsonNode page = data(getJson("/admin/onboardings", adminToken, Map.of("pageSize", "10")));
        assertEquals(1, page.path("total").asLong());
        assertEquals("110****2345", page.path("records").get(0).path("legalRepIdNo").asText());
        assertFalse(page.toString().contains(LEGAL_REP_ID_NO));

        JsonNode detail = data(getJson("/admin/onboardings/" + onboardingId, adminToken));
        assertEquals(LEGAL_REP_ID_NO, detail.path("legalRepIdNo").asText());
        assertEquals(2, detail.path("files").size());

        assertTrue(jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_notification WHERE receiver_account_id = ? AND notice_type = 'ONBOARDING_PENDING'",
                Long.class, adminAccountId()) > 0);

        postJson("/api/onboarding/submit", token,
                onboardingBody("北京顺达货主", "911101010000123456", LEGAL_REP_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("审核中")));

        getJson("/admin/onboardings", token).andExpect(status().isForbidden());
        assertThrows(BusinessException.class,
                () -> onboardingService.assertApproved(shipperAccountId, AccountRoleEnum.SHIPPER));
    }

    @Test
    void adminApproveRecordsReviewerAndEnablesEligibility() throws Exception {
        Long shipperAccountId = seedShipper("13900000003");
        String token = login("13900000003", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        long onboardingId = data(postJson("/api/onboarding/submit", token,
                onboardingBody("上海承运货主", "913101010000123456", LEGAL_REP_ID_NO))).path("id").asLong();

        postJson("/admin/onboardings/" + onboardingId + "/approve", adminToken, Map.of())
                .andExpect(status().isOk());

        JsonNode mine = data(getJson("/api/onboarding", token));
        assertEquals("APPROVED", mine.path("auditStatus").asText());
        assertTrue(mine.hasNonNull("reviewerAccountId"));
        assertTrue(mine.hasNonNull("reviewTime"));

        assertDoesNotThrow(() -> onboardingService.assertApproved(shipperAccountId, AccountRoleEnum.SHIPPER));

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'ONBOARDING_APPROVE' AND target_id = ?",
                Long.class, onboardingId));

        postJson("/admin/onboardings/" + onboardingId + "/approve", adminToken, Map.of())
                .andExpect(status().isBadRequest());

        postJson("/api/onboarding/submit", token,
                onboardingBody("上海承运货主", "913101010000123456", LEGAL_REP_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("已通过")));
    }

    @Test
    void adminRejectRequiresReasonAndAllowsNewApplication() throws Exception {
        seedShipper("13900000004");
        String token = login("13900000004", DEFAULT_PASSWORD);
        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        long onboardingId = data(postJson("/api/onboarding/submit", token,
                onboardingBody("广州货主", "914401010000123456", LEGAL_REP_ID_NO))).path("id").asLong();

        postJson("/admin/onboardings/" + onboardingId + "/reject", adminToken, Map.of())
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("驳回原因不能为空")));

        postJson("/admin/onboardings/" + onboardingId + "/reject", adminToken, Map.of("reason", "资质附件不清晰"))
                .andExpect(status().isOk());

        JsonNode rejected = data(getJson("/api/onboarding", token));
        assertEquals("REJECTED", rejected.path("auditStatus").asText());
        assertEquals("资质附件不清晰", rejected.path("rejectReason").asText());

        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_operation_log WHERE action_type = 'ONBOARDING_REJECT' AND target_id = ?",
                Long.class, onboardingId));

        JsonNode resubmitted = data(postJson("/api/onboarding/submit", token,
                onboardingBody("广州货主", "914401010000123456", LEGAL_REP_ID_NO)));
        assertTrue(resubmitted.path("id").asLong() != onboardingId);
        assertEquals("PENDING", resubmitted.path("auditStatus").asText());

        JsonNode page = data(getJson("/admin/onboardings", adminToken, Map.of("pageSize", "10")));
        assertEquals(2, page.path("total").asLong());
        assertEquals("PENDING", data(getJson("/api/onboarding", token)).path("auditStatus").asText());
    }

    @Test
    void legalRepIdentityIsIndependentFromPersonalRealname() throws Exception {
        seedShipper("13900000005");
        String firstToken = login("13900000005", DEFAULT_PASSWORD);
        data(postJson("/api/onboarding/submit", firstToken,
                onboardingBody("深圳货主甲", "914403010000123456", "110101197701017777")));

        JsonNode personal = data(getJson("/api/realname", firstToken));
        assertEquals("110101199001010005", personal.path("idNo").asText());
        assertEquals("PERSONAL", personal.path("realnameType").asText());

        String adminToken = login(ADMIN_PHONE, ADMIN_PASSWORD);
        JsonNode page = data(getJson("/admin/realnames", adminToken, Map.of("role", "SHIPPER", "pageSize", "10")));
        assertEquals(1, page.path("total").asLong());
        assertFalse(page.toString().contains("110101197701017777"));

        seedShipper("13900000006");
        String secondToken = login("13900000006", DEFAULT_PASSWORD);
        postJson("/api/onboarding/submit", secondToken,
                onboardingBody("深圳货主乙", "914403010000654321", "110101197701017777"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("该实名身份已绑定其他账号")));
    }

    @Test
    void samePersonCanBePersonalAccountAndLegalRep() throws Exception {
        seedShipper("13900000008");
        String token = login("13900000008", DEFAULT_PASSWORD);
        JsonNode submitted = data(postJson("/api/onboarding/submit", token,
                onboardingBody("同一自然人公司", "914403010000888888", "110101199001010008")));
        assertEquals("PENDING", submitted.path("auditStatus").asText());
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_realname WHERE realname_type = 'PERSONAL' AND id_no = ?",
                Long.class, "110101199001010008"));
        assertEquals(1L, jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM biz_realname WHERE realname_type = 'LEGAL_REP' AND id_no = ?",
                Long.class, "110101199001010008"));
    }

    @Test
    void enterpriseAndLegalRepVerificationFailuresBlockSubmission() throws Exception {
        seedShipper("13900000007");
        String token = login("13900000007", DEFAULT_PASSWORD);

        postJson("/api/onboarding/submit", token,
                onboardingBody("信用异常公司", "9111010100009999", LEGAL_REP_ID_NO))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("模拟校验：企业主体信息不匹配")));
        assertTrue(data(getJson("/api/onboarding", token)).isNull());

        postJson("/api/onboarding/submit", token,
                onboardingBody("法人异常公司", "911101010000123456", "110101198001019999"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("法定代表人实名校验未通过")));
        assertTrue(data(getJson("/api/onboarding", token)).isNull());
    }

    private Map<String, Object> onboardingBody(String subjectName, String creditCode, String legalRepIdNo) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("subjectName", subjectName);
        body.put("creditCode", creditCode);
        body.put("legalRepName", "法人代表");
        body.put("legalRepIdType", "ID_CARD");
        body.put("legalRepIdNo", legalRepIdNo);
        body.put("contactName", "业务联系人");
        body.put("contactPhone", "13900000001");
        body.put("province", "北京市");
        body.put("city", "北京市");
        body.put("district", "朝阳区");
        body.put("businessScope", "普通货运");
        body.put("attachmentPaths", List.of("/files/onboarding/pdf/20261107/a.pdf",
                "/files/onboarding/image/20261107/b.jpg"));
        return body;
    }

    private String registerAndLogin(String role, String phone, String name, String password) throws Exception {
        mockMvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders
                        .post("/auth/register")
                        .contentType(org.springframework.http.MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(Map.of(
                                "role", role, "phone", phone, "name", name,
                                "email", phone + "@example.com", "password", password))))
                .andExpect(status().isOk());
        return login(phone, password);
    }

    private Long adminAccountId() {
        return jdbcTemplate.queryForObject("SELECT id FROM sys_account WHERE role = 'ADMIN' ORDER BY id LIMIT 1",
                Long.class);
    }
}
