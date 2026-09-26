package com.farmconnect.service.impl;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.farmconnect.config.CloudinaryConfig;
import com.farmconnect.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Handles persistent image storage for products and farmer profiles.
 * If Cloudinary credentials are configured via environment variables, images are
 * uploaded to Cloudinary and their secure HTTPS URLs are returned.
 * If Cloudinary is not configured (e.g. during local tests), it falls back to
 * local filesystem storage under app.upload.dir (/uploads/<filename>).
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class FileStorageServiceImpl {

    private final Cloudinary cloudinary;
    private final CloudinaryConfig cloudinaryConfig;

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }

        if (cloudinaryConfig != null && cloudinaryConfig.isConfigured()) {
            return storeToCloudinary(file);
        }

        return storeToLocal(file);
    }

    private String storeToCloudinary(MultipartFile file) {
        try {
            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", "farmconnect/products",
                            "resource_type", "image"
                    )
            );
            String secureUrl = (String) uploadResult.get("secure_url");
            if (secureUrl == null || secureUrl.isBlank()) {
                throw new BadRequestException("Cloudinary did not return a valid secure URL");
            }
            log.info("Image uploaded successfully to Cloudinary");
            return secureUrl;
        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage());
            throw new BadRequestException("Failed to store file");
        }
    }

    private String storeToLocal(MultipartFile file) {
        try {
            Path dirPath = Paths.get(uploadDir);
            if (!Files.exists(dirPath)) {
                Files.createDirectories(dirPath);
            }
            String original = Objects.requireNonNullElse(file.getOriginalFilename(), "file");
            String ext = original.contains(".") ? original.substring(original.lastIndexOf('.')) : "";
            String filename = UUID.randomUUID() + ext;
            Path target = dirPath.resolve(filename);
            Files.copy(file.getInputStream(), target);
            return "/uploads/" + filename;
        } catch (IOException e) {
            log.error("Failed to store file locally: {}", e.getMessage());
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }
}
