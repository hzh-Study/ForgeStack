package com.example.web.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.HexFormat;

/**
 * 密码工具类，负责基础账号体系的盐值生成、密码加密和密码校验。
 */
public final class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    private PasswordUtil() {
    }

    /**
     * 生成密码盐值。
     *
     * @return 十六进制盐值
     */
    public static String generateSalt() {
        byte[] bytes = new byte[16];
        RANDOM.nextBytes(bytes);
        return HexFormat.of().formatHex(bytes);
    }

    /**
     * 使用 SHA-256 对密码和盐值进行加密。
     *
     * @param password 明文密码
     * @param salt 密码盐值
     * @return 密码摘要
     */
    public static String hashPassword(String password, String salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest((password + ":" + salt).getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("当前 JDK 不支持 SHA-256", exception);
        }
    }

    /**
     * 校验明文密码是否和数据库摘要一致。
     *
     * @param password 明文密码
     * @param salt 密码盐值
     * @param passwordHash 数据库存储的密码摘要
     * @return true 表示密码正确
     */
    public static boolean matches(String password, String salt, String passwordHash) {
        return hashPassword(password, salt).equals(passwordHash);
    }
}
