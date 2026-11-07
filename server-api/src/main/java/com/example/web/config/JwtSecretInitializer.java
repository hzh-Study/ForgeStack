package com.example.web.config;

import cn.dev33.satoken.SaManager;
import cn.dev33.satoken.stp.StpLogic;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * JWT 密钥初始化器。
 *
 * <p>优先使用 JWT_SECRET 环境变量；未配置时在用户目录生成并持久化随机密钥，
 * 避免在仓库中提交固定密钥，同时保证本地开发重启后旧 Token 仍然可用。</p>
 */
@Component
public class JwtSecretInitializer implements SmartInitializingSingleton {

    private static final Logger LOGGER = LoggerFactory.getLogger(JwtSecretInitializer.class);

    /**
     * 自动生成密钥的默认目录。
     */
    private static final String DEFAULT_SECRET_DIRECTORY = ".forgestack";

    /**
     * 自动生成密钥的默认文件名。
     */
    private static final String DEFAULT_SECRET_FILE = "jwt-secret";

    private static final int SECRET_BYTES = 48;

    private final Environment environment;

    private final StpLogic stpLogic;

    /**
     * 密钥文件路径，未配置时使用用户目录下的默认路径。
     */
    @Value("${app.jwt.secret-file:}")
    private String secretFile;

    public JwtSecretInitializer(Environment environment, StpLogic stpLogic) {
        this.environment = environment;
        this.stpLogic = stpLogic;
    }

    /**
     * 在所有单例创建完成后写入 JWT 密钥，避免被 Sa-Token 自动配置覆盖。
     */
    @Override
    public void afterSingletonsInstantiated() {
        String secret = resolveSecretFromEnvironment();
        if (secret == null || secret.isBlank()) {
            Path file = resolveSecretFile();
            secret = loadOrCreateSecret(file);
            LOGGER.warn("JWT_SECRET is not configured; using local secret file: {}; set JWT_SECRET in production", file);
        } else {
            LOGGER.info("JWT secret loaded from environment variable");
        }
        if (secret.length() < 32) {
            LOGGER.warn("JWT_SECRET is shorter than 32 characters; use a longer random secret");
        }
        SaManager.getConfig().setJwtSecretKey(secret);
        if (stpLogic.getConfig() != null) {
            stpLogic.getConfig().setJwtSecretKey(secret);
        }
    }

    /**
     * 从环境变量或配置项读取 JWT 密钥。
     *
     * @return 密钥，未配置时返回 null
     */
    private String resolveSecretFromEnvironment() {
        String secret = environment.getProperty("JWT_SECRET");
        if (secret == null || secret.isBlank()) {
            secret = environment.getProperty("sa-token.jwt-secret-key");
        }
        return secret == null ? null : secret.trim();
    }

    /**
     * 解析密钥文件路径。
     *
     * @return 密钥文件路径
     */
    private Path resolveSecretFile() {
        if (secretFile != null && !secretFile.isBlank()) {
            return Paths.get(secretFile.trim());
        }
        return Paths.get(System.getProperty("user.home"), DEFAULT_SECRET_DIRECTORY, DEFAULT_SECRET_FILE);
    }

    /**
     * 读取已有密钥，不存在时生成并写入。
     *
     * @param file 密钥文件路径
     * @return JWT 密钥
     */
    private String loadOrCreateSecret(Path file) {
        try {
            if (Files.exists(file)) {
                String existing = Files.readString(file, StandardCharsets.UTF_8).trim();
                if (!existing.isBlank()) {
                    return existing;
                }
            }
            Path parent = file.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            String generated = generateSecret();
            try {
                Files.writeString(file, generated, StandardCharsets.UTF_8);
            } catch (FileAlreadyExistsException exception) {
                return Files.readString(file, StandardCharsets.UTF_8).trim();
            }
            return generated;
        } catch (IOException exception) {
            throw new IllegalStateException("初始化 JWT 密钥失败，请检查 JWT_SECRET 或 JWT_SECRET_FILE 配置", exception);
        }
    }

    /**
     * 生成 URL 安全的随机密钥。
     *
     * @return 随机密钥
     */
    private String generateSecret() {
        byte[] bytes = new byte[SECRET_BYTES];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }
}
