package com.shop.common.fileUpload.service;

import java.io.IOException;

import org.springframework.web.multipart.MultipartFile;

public interface FileUploadService {

    String uploadFile(MultipartFile file) throws IOException;

    String uploadFile(MultipartFile file, String path) throws IOException;

    void deleteFile(String path) throws IOException;
}
