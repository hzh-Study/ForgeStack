package com.example.web.config;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * 启动期数据库结构守卫。
 *
 * <p>应用启动后、业务定时任务首次执行前，依次校验平台核心表是否可查询，
 * 避免连接到错误的数据库或未初始化数据库时，问题只在定时任务中通过异常堆栈暴露。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class SchemaGuard implements ApplicationRunner {

    private static final Logger LOGGER = LoggerFactory.getLogger(SchemaGuard.class);

    /**
     * 平台核心表清单，覆盖账号、管理员、订单、任务、打卡、报销与平台参数。
     */
    private static final List<String> REQUIRED_TABLES = List.of(
            "sys_account",
            "sys_admin",
            "biz_order",
            "biz_task",
            "biz_checkin",
            "biz_reimbursement",
            "sys_platform_param");

    /**
     * 数据库未初始化或连错库时面向排查人员的可操作提示，不暴露数据库密码与异常堆栈。
     */
    private static final String DATABASE_NOT_READY_MESSAGE =
            "当前数据库未初始化或连接到了错误的库，请执行 server-api/首次初始化数据库脚本.sql，"
                    + "并确认 DB_NAME（默认 forgestack）指向本平台数据库";

    private final JdbcTemplate jdbcTemplate;

    /**
     * 注入数据库访问模板，用于执行只读表校验。
     *
     * @param jdbcTemplate Spring JDBC 模板
     */
    public SchemaGuard(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 应用启动入口：逐表校验核心表，任一表查询失败立即终止启动。
     *
     * @param args 启动参数
     */
    @Override
    public void run(ApplicationArguments args) {
        for (String tableName : REQUIRED_TABLES) {
            verifyTable(tableName);
        }
        LOGGER.info("数据库核心表校验通过，共 {} 张", REQUIRED_TABLES.size());
    }

    /**
     * 通过只读计数查询验证单表是否存在且可访问。
     *
     * @param tableName 待校验表名
     */
    private void verifyTable(String tableName) {
        try {
            jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tableName, Long.class);
        } catch (Exception exception) {
            Throwable rootCause = resolveRootCause(exception);
            LOGGER.error("核心表 {} 无法查询，根因：{}", tableName, rootCause.getMessage());
            throw new IllegalStateException(DATABASE_NOT_READY_MESSAGE + "（校验失败的表：" + tableName + "）");
        }
    }

    /**
     * 逐层查找最底层异常，便于日志只记录根因而不打印完整堆栈。
     *
     * @param exception 原始异常
     * @return 根因异常
     */
    private Throwable resolveRootCause(Throwable exception) {
        Throwable cause = exception;
        while (cause.getCause() != null && cause.getCause() != cause) {
            cause = cause.getCause();
        }
        return cause;
    }
}
