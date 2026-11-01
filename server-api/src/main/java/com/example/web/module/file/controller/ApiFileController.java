package com.example.web.module.file.controller;

import cn.dev33.satoken.annotation.SaCheckPermission;
import com.example.web.common.result.Result;
import com.example.web.module.file.service.FileStorageService;
import com.example.web.module.file.vo.FileUploadVO;
import com.example.web.security.PermissionCode;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

/**
 * 前台通用文件上传接口。
 */
@RestController
@RequestMapping("/api/files")
public class ApiFileController {

    private final FileStorageService fileStorageService;

    public ApiFileController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    /**
     * 批量上传文件。
     *
     * @param files 文件数组
     * @param module 模块标识
     * @param fileType 文件类型
     * @return 上传结果
     */
    @PostMapping("/upload")
    @SaCheckPermission(PermissionCode.WEB_FILE_UPLOAD)
    public Result<List<FileUploadVO>> upload(@RequestParam("file") MultipartFile[] files,
                                             @RequestParam(value = "module", required = false) String module,
                                             @RequestParam(value = "fileType", required = false) String fileType) {
        return Result.success(fileStorageService.upload(files, module, fileType));
    }
}
