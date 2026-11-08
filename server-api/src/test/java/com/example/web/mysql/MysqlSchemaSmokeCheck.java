package com.example.web.mysql;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

/**
 * MySQL 真实数据库冒烟检查。
 *
 * <p>类名故意不以 Test/Tests 结尾，默认 surefire 不会执行；仅在 mysql-check profile 中运行，
 * 用于验证 MySQL 保留字、utf8mb4 索引长度与 SQL 初始化编码是否正常。</p>
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("mysql")
class MysqlSchemaSmokeCheck {

    /**
     * 中文校验正则，用于确认 utf8mb4 数据未在初始化时乱码。
     */
    private static final Pattern CHINESE_PATTERN = Pattern.compile("[\\u4e00-\\u9fa5]");

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * 校验种子与演示数据条数、中文内容，并通过登录接口验证 API 与 JSON 编码。
     */
    @Test
    void shouldLoadSchemaAndDemoDataOnMysql() throws Exception {
        assertEquals(14L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM sys_account", Long.class));
        assertEquals(11L, jdbcTemplate.queryForObject("SELECT COUNT(*) FROM biz_order", Long.class));
        assertEquals("北京华远商贸有限公司",
                jdbcTemplate.queryForObject("SELECT name FROM biz_shipper WHERE id = 2", String.class));
        assertEquals("精密机械设备",
                jdbcTemplate.queryForObject(
                        "SELECT goods_name FROM biz_order_goods ORDER BY id LIMIT 1", String.class));

        String description = jdbcTemplate.queryForObject(
                "SELECT description FROM sys_platform_param WHERE param_key = 'order.default.valid.hours'",
                String.class);
        assertNotNull(description);
        assertTrue(CHINESE_PATTERN.matcher(description).find(), "平台参数描述应包含中文，实际：" + description);

        String responseBody = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(
                                Map.of("username", "13800000000", "password", "admin123")))
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(StandardCharsets.UTF_8);
        JsonNode data = objectMapper.readTree(responseBody).path("data");
        assertEquals("平台管理员", data.path("displayName").asText());
    }
}
