package com.example.web.module.file.service;

import com.example.web.module.file.vo.FileUploadVO;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;

/**
 * 通用文件存储服务。
 */
public interface FileStorageService {

    /**
     * 批量上传文件并记录上传者，用于后续鉴权读取。
     *
     * @param files 上传文件
     * @param module 模块标识
     * @param fileType 文件类型
     * @return 上传结果
     */
    List<FileUploadVO> upload(MultipartFile[] files, String module, String fileType);
}
