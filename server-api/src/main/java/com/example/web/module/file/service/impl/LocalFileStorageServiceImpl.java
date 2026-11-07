package com.example.web.module.file.service.impl;

import com.example.web.exception.BusinessException;
import com.example.web.module.file.entity.StoredFile;
import com.example.web.module.file.mapper.StoredFileMapper;
import com.example.web.module.file.service.FileStorageService;
import com.example.web.module.file.vo.FileUploadVO;
import com.example.web.security.CurrentAccountService;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 本地文件存储实现，上传成功后写入文件记录供鉴权接口校验。
 */
@Service
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMdd");

    @Value("${app.file.upload-dir}")
    private String uploadDir;

    @Value("${app.file.access-prefix}")
    private String accessPrefix;

    /**
     * 单文件大小上限，单位为字节。
     */
    @Value("${app.file.max-size:10485760}")
    private long maxFileSize;

    /**
     * 允许上传的扩展名，逗号分隔。
     */
    @Value("${app.file.allowed-extensions:jpg,jpeg,png,gif,webp,bmp,pdf,doc,docx,xls,xlsx,ppt,pptx,zip,rar,txt}")
    private String allowedExtensions;

    private Set<String> allowedExtensionSet = Set.of();

    private final StoredFileMapper storedFileMapper;
    private final CurrentAccountService currentAccountService;

    public LocalFileStorageServiceImpl(StoredFileMapper storedFileMapper, CurrentAccountService currentAccountService) {
        this.storedFileMapper = storedFileMapper;
        this.currentAccountService = currentAccountService;
    }

    @PostConstruct
    public void initAllowedExtensions() {
        allowedExtensionSet = Arrays.stream(allowedExtensions.split(","))
                .map(String::trim)
                .map(item -> item.toLowerCase(Locale.ROOT))
                .filter(item -> !item.isBlank())
                .collect(Collectors.toUnmodifiableSet());
    }

    @Override
    public List<FileUploadVO> upload(MultipartFile[] files, String module, String fileType) {
        if (files == null || files.length == 0) {
            throw new BusinessException("文件不能为空");
        }
        Long uploaderAccountId = currentAccountService.getBusinessAccount().getId();
        List<FileUploadVO> result = new ArrayList<>();
        for (MultipartFile file : files) {
            result.add(saveFile(file, module, fileType, uploaderAccountId));
        }
        return result;
    }

    /**
     * 保存单个上传文件并写入文件记录。
     *
     * @param file 上传文件
     * @param module 业务模块
     * @param fileType 文件类型
     * @param uploaderAccountId 上传账号 ID
     * @return 文件上传返回信息
     */
    private FileUploadVO saveFile(MultipartFile file, String module, String fileType, Long uploaderAccountId) {
        if (file == null || file.isEmpty() || file.getSize() <= 0) {
            throw new BusinessException("上传的文件不能为空");
        }
        String originalName = sanitizeOriginalName(file.getOriginalFilename());
        String extension = getExtension(originalName);
        validateFile(file, extension);
        String fileName = UUID.randomUUID().toString().replace("-", "") + extension;
        String safeModule = sanitizePathPart(module, "common");
        String safeFileType = sanitizePathPart(fileType, "file");
        String datePath = LocalDate.now().format(DATE_FORMATTER);
        Path directory = Paths.get(uploadDir, safeModule, safeFileType, datePath).normalize();
        Path targetPath = directory.resolve(fileName).normalize();
        try {
            Files.createDirectories(directory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath);
            }
        } catch (IOException exception) {
            throw new BusinessException("文件上传失败");
        }
        String url = String.join("/", accessPrefix, safeModule, safeFileType, datePath, fileName).replace("\\", "/");

        StoredFile storedFile = new StoredFile();
        storedFile.setFilePath(url);
        storedFile.setOriginalName(originalName);
        storedFile.setUploaderAccountId(uploaderAccountId);
        storedFile.setFileModule(safeModule);
        storedFile.setFileType(safeFileType);
        storedFileMapper.insert(storedFile);

        return new FileUploadVO(url, originalName, fileName, file.getSize(), file.getContentType());
    }

    /**
     * 清理原始文件名，去除目录部分，空文件名使用 unknown。
     *
     * @param originalName 原始文件名
     * @return 安全的原始文件名
     */
    private String sanitizeOriginalName(String originalName) {
        if (originalName == null || originalName.isBlank()) {
            return "unknown";
        }
        return Paths.get(originalName).getFileName().toString();
    }

    /**
     * 提取小写扩展名，无扩展名时返回空字符串。
     *
     * @param originalName 原始文件名
     * @return 带点扩展名或空字符串
     */
    private String getExtension(String originalName) {
        int index = originalName.lastIndexOf('.');
        if (index < 0 || index == originalName.length() - 1) {
            return "";
        }
        return originalName.substring(index).toLowerCase();
    }

    /**
     * 校验文件大小和扩展名，阻止可执行脚本和超大文件上传。
     *
     * @param file 上传文件
     * @param extension 带点扩展名
     */
    private void validateFile(MultipartFile file, String extension) {
        if (maxFileSize > 0 && file.getSize() > maxFileSize) {
            long maxMegabytes = Math.max(1, maxFileSize / (1024 * 1024));
            throw new BusinessException("文件大小不能超过 " + maxMegabytes + "MB");
        }
        String normalizedExtension = extension.startsWith(".")
                ? extension.substring(1).toLowerCase(Locale.ROOT)
                : extension.toLowerCase(Locale.ROOT);
        if (normalizedExtension.isBlank() || !allowedExtensionSet.contains(normalizedExtension)) {
            throw new BusinessException("不支持的文件类型，仅允许："
                    + String.join(",", allowedExtensionSet));
        }
    }

    /**
     * 清理路径片段，只保留字母、数字、下划线与短横线。
     *
     * @param value 原始路径片段
     * @param defaultValue 清理后为空时的默认值
     * @return 安全的路径片段
     */
    private String sanitizePathPart(String value, String defaultValue) {
        if (value == null || value.isBlank()) {
            return defaultValue;
        }
        String sanitized = value.replaceAll("[^a-zA-Z0-9_-]", "");
        return sanitized.isBlank() ? defaultValue : sanitized;
    }
}
