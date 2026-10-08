package com.chat.domain.file;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.*;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/files")
public class FileController {

    @Value("${file.upload-dir:uploads}")
    private String uploadDir;

    @Value("${file.base-url:/uploads}")
    private String baseUrl;

    @PostMapping("/upload")
    public Map<String, Object> upload(@RequestParam("file") MultipartFile file) throws Exception {
        if (file.isEmpty()) throw new IllegalArgumentException("파일 없음");
        if (file.getSize() > 10 * 1024 * 1024) throw new IllegalArgumentException("10MB 초과");
        Path dir = Paths.get(uploadDir);
        Files.createDirectories(dir);
        String ext = "";
        String orig = file.getOriginalFilename() != null ? file.getOriginalFilename() : "file";
        int dot = orig.lastIndexOf('.');
        if (dot >= 0) ext = orig.substring(dot);
        String saved = UUID.randomUUID() + ext;
        Files.copy(file.getInputStream(), dir.resolve(saved), StandardCopyOption.REPLACE_EXISTING);
        return Map.of("url", baseUrl + "/" + saved, "fileName", orig, "size", file.getSize());
    }
}
