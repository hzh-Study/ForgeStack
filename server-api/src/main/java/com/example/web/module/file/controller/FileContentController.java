package com.example.web.module.file.controller;

import com.example.web.module.file.service.FileAccessService;
import java.nio.file.Path;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 鉴权文件读取接口，前台与后台共用同一套数据范围校验。
 */
@RestController
public class FileContentController {

    private final FileAccessService fileAccessService;

    public FileContentController(FileAccessService fileAccessService) {
        this.fileAccessService = fileAccessService;
    }

    /**
     * 按角色与数据范围读取文件内容。
     *
     * @param path 文件相对路径
     * @return 文件内容
     */
    @GetMapping({"/api/files/view", "/admin/files/view"})
    public ResponseEntity<Resource> view(@RequestParam("path") String path) {
        Path target = fileAccessService.resolveReadable(path);
        try {
            Resource resource = new UrlResource(target.toUri());
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-store")
                    .contentType(MediaType.parseMediaType(fileAccessService.getContentType(target)))
                    .body(resource);
        } catch (java.net.MalformedURLException exception) {
            throw new com.example.web.exception.BusinessException("文件不存在");
        }
    }
}
