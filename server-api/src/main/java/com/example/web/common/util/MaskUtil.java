package com.example.web.common.util;

/**
 * 隐私字段脱敏工具。
 */
public final class MaskUtil {

    private MaskUtil() {
    }

    /**
     * 证件号脱敏，保留前 3 位与后 4 位。
     *
     * @param idNo 完整证件号
     * @return 脱敏后的证件号
     */
    public static String maskIdNo(String idNo) {
        if (idNo == null || idNo.isBlank()) {
            return "";
        }
        String value = idNo.trim();
        if (value.length() <= 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }

    /**
     * 手机号脱敏，保留前 3 位与后 4 位。
     *
     * @param phone 手机号
     * @return 脱敏后的手机号
     */
    public static String maskPhone(String phone) {
        if (phone == null || phone.isBlank()) {
            return "";
        }
        String value = phone.trim();
        if (value.length() <= 7) {
            return "***";
        }
        return value.substring(0, 3) + "****" + value.substring(value.length() - 4);
    }
}
