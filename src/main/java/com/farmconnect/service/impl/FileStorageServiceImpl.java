package com.farmconnect.service.impl;

import com.farmconnect.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.UUID;
import java.util.Objects;

/**
 * Stores uploaded images on local disk under app.upload.dir and serves them
 * back under /uploads/<filename> (see WebConfig for the static resource
 * mapping).
 *
 * IMPORTANT: this returns a RELATIVE path ("/uploads/<filename>"), not an
 * absolute URL. Earlier versions baked app.upload.base-url (a static
 * property) into the stored/returned URL at upload time - that meant
 * whichever host happened to be configured *at the moment a product's
 * image was uploaded* was permanently frozen into that row. Any time the
 * backend's IP changed afterwards (new Wi-Fi, different machine, etc.),
 * every image uploaded before that change still carried the old host.
 * Android's ImageUrlHelper compensates for that on old rows by stripping
 * whatever host is present and rebuilding against its own current
 * ApiClient.BASE_URL - but that only works if app.upload.base-url in this
 * file's properties was already updated to match by the time a *new*
 * upload happens; if it hadn't been, brand-new uploads would bake in yet
 * another (still wrong, or differently wrong) host. A relative path has no
 * host at all, so there's nothing to go stale on either side, ever again -
 * Android always builds the full URL from whatever backend it's currently
 * pointed at, for every product regardless of when its image was uploaded.
 */
@Service
@RequiredArgsConstructor
public class FileStorageServiceImpl {

    @Value("${app.upload.dir}")
    private String uploadDir;

    public String store(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BadRequestException("No file provided");
        }
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
            throw new BadRequestException("Failed to store file: " + e.getMessage());
        }
    }
}
