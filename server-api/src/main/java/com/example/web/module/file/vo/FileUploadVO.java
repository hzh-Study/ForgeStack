package com.example.web.module.file.vo;

/**
 * 文件上传返回信息。
 */
public class FileUploadVO {

    /**
     * 文件相对访问路径
     */
    private String url;

    /**
     * 上传时的原始文件名
     */
    private String originalName;

    /**
     * 存储后的文件名
     */
    private String fileName;

    /**
     * 文件大小，单位为字节
     */
    private Long size;

    /**
     * 文件内容类型
     */
    private String contentType;

    /**
     * 构造文件上传返回信息。
     *
     * @param url 文件相对访问路径
     * @param originalName 原始文件名
     * @param fileName 存储文件名
     * @param size 文件大小
     * @param contentType 文件内容类型
     */
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
