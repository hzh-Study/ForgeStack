package com.example.web.security;

import cn.dev33.satoken.stp.StpUtil;
import com.example.web.exception.BusinessException;

/**
 * Sa-Token 登录账号工具，统一使用账号表主键维护登录态。
 */
public final class LoginAccountUtil {

    /**
     * 登录 ID 前缀，用于区分系统账号和其他潜在登录体系。
     */
    private static final String ACCOUNT_PREFIX = "account_";

    private LoginAccountUtil() {
    }

    /**
     * 使用统一账号 ID 建立登录态。
     *
     * @param accountId 账号表主键
     */
    public static void login(Long accountId) {
        StpUtil.login(ACCOUNT_PREFIX + accountId);
    }

    /**
     * 获取当前 Token 值。
     *
     * @return Token 值
     */
    public static String getTokenValue() {
        return StpUtil.getTokenValue();
    }

    /**
     * 从当前登录态解析统一账号 ID。
     *
     * @return 账号表主键
     */
    public static Long getCurrentAccountId() {
        return parseAccountId(StpUtil.getLoginIdAsString());
    }

    /**
     * 从 Sa-Token 登录 ID 中解析统一账号主键。
     *
     * @param loginId Sa-Token 登录 ID
     * @return 账号表主键
     */
    public static Long parseAccountId(Object loginId) {
        if (loginId == null || !loginId.toString().startsWith(ACCOUNT_PREFIX)) {
            throw new BusinessException("登录已失效");
        }
        try {
            return Long.parseLong(loginId.toString().substring(ACCOUNT_PREFIX.length()));
        } catch (NumberFormatException exception) {
            throw new BusinessException("登录已失效");
        }
    }
}
