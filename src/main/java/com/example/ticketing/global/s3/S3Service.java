package com.example.ticketing.global.s3;

import io.awspring.cloud.s3.S3Template;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Template s3Template;

    @Value("${app.s3.bucket}")
    private String bucketName;

    public String uploadPoster(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return null;
        }

        String originalFilename = file.getOriginalFilename();
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
        }
        String savedFilename = "posters/" + UUID.randomUUID().toString() + extension;

        try (InputStream inputStream = file.getInputStream()) {
            var resource = s3Template.upload(bucketName, savedFilename, inputStream, null);
            log.info("Successfully uploaded poster image to S3: {}/{}", bucketName, savedFilename);
            
            // Return public URL of the uploaded image
            return resource.getURL().toString();
        } catch (IOException e) {
            log.error("Failed to upload file to S3", e);
            throw new RuntimeException("S3 파일 업로드 실패", e);
        }
    }
}
