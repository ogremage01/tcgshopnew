package com.shop.common.fileUpload.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.File;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
@Service
public class FileUploadServiceImpl implements FileUploadService {

    @Value("${file.path.upload}")
    private String uploadRootPath;


    @Override
    public String uploadFile(MultipartFile file) throws IOException {
        return uploadFile(file, "");
    }

    @Override
    public String uploadFile(MultipartFile file, String targetPath) throws IOException {
        String originalFileName = file.getOriginalFilename() == null ? "upload" : file.getOriginalFilename();
        String storedFileName = UUID.randomUUID() + "_" + originalFileName;
        String normalizedTarget = targetPath == null ? "" : targetPath.replace("\\", "/").replaceAll("^/+|/+$", "");
        String rootPath = uploadRootPath.endsWith("/") || uploadRootPath.endsWith("\\")
            ? uploadRootPath
            : uploadRootPath + "/";
        String fullPath = rootPath + (normalizedTarget.isEmpty() ? "" : normalizedTarget + "/") + storedFileName;
        File dest = new File(fullPath);

        File Parent = dest.getParentFile();
        if (!Parent.exists()) {
            Parent.mkdirs();
        }
        file.transferTo(dest);

        String relativePath = fullPath.replace(rootPath, "").replace("\\", "/").replaceAll("^/+", "");
        return "uploads/" + relativePath;
    }

    @Override
    public void deleteFile(String path) throws IOException {
        File file = new File(path);
        if (file.exists()) {
            file.delete();
        }
    }
}
