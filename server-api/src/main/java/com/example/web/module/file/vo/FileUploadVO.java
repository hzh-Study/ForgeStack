package com.example.web.module.file.vo;

/**
 * 文件上传返回信息。
 */
public class FileUploadVO {

    private String url;

    private String originalName;

    private String fileName;

    private Long size;

    private String contentType;

    public FileUploadVO(String url, String originalName, String fileName, Long size, String contentType) {
        this.url = url;
        this.originalName = originalName;
        this.fileName = fileName;
        this.size = size;
        this.contentType = contentType;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getOriginalName() {
        return originalName;
    }

    public void setOriginalName(String originalName) {
        this.originalName = originalName;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public Long getSize() {
        return size;
    }

    public void setSize(Long size) {
        this.size = size;
    }

    public String getContentType() {
        return contentType;
    }

    public void setContentType(String contentType) {
        this.contentType = contentType;
    }
}
