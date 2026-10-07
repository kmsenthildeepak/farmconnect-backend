package com.farmconnect.config;

import com.cloudinary.Cloudinary;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.service.impl.FileStorageServiceImpl;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class CloudinaryConfigTest {

    @Test
    @DisplayName("Verify CloudinaryConfig reports not configured when credentials are blank")
    void testNotConfiguredByDefault() {
        CloudinaryConfig config = new CloudinaryConfig();

        assertFalse(config.isConfigured());

        Cloudinary cloudinary = config.cloudinary();

        assertNotNull(cloudinary);
    }

    @Test
    @DisplayName("Verify CloudinaryConfig reports configured when credentials are set")
    void testConfiguredWhenPropertiesProvided() {
        CloudinaryConfig config = new CloudinaryConfig();

        ReflectionTestUtils.setField(config, "cloudName", "test-cloud");
        ReflectionTestUtils.setField(config, "apiKey", "test-key");
        ReflectionTestUtils.setField(config, "apiSecret", "test-secret");

        assertTrue(config.isConfigured());

        Cloudinary cloudinary = config.cloudinary();

        assertNotNull(cloudinary);
        assertEquals("test-cloud", cloudinary.config.cloudName);
        assertEquals("test-key", cloudinary.config.apiKey);
        assertEquals("test-secret", cloudinary.config.apiSecret);
        assertTrue(cloudinary.config.secure);
    }

    @Test
    @DisplayName("Verify JPEG image is stored with a server-generated safe filename")
    void testFallbackToLocalStorage(@TempDir Path tempDir) {
        CloudinaryConfig config = new CloudinaryConfig();
        Cloudinary cloudinary = config.cloudinary();

        FileStorageServiceImpl storageService =
                new FileStorageServiceImpl(cloudinary, config);

        ReflectionTestUtils.setField(
                storageService,
                "uploadDir",
                tempDir.toString()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test-image.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        String resultPath = storageService.store(file);

        assertNotNull(resultPath);
        assertTrue(resultPath.startsWith("/uploads/"));
        assertTrue(resultPath.endsWith(".jpg"));

        String filename = resultPath.substring("/uploads/".length());

        assertDoesNotThrow(() ->
                java.util.UUID.fromString(
                        filename.substring(0, filename.length() - 4)
                )
        );

        assertTrue(Files.exists(tempDir.resolve(filename)));
    }

    @Test
    @DisplayName("Reject non-image files")
    void testRejectsNonImageFile(@TempDir Path tempDir) {
        CloudinaryConfig config = new CloudinaryConfig();
        Cloudinary cloudinary = config.cloudinary();

        FileStorageServiceImpl storageService =
                new FileStorageServiceImpl(cloudinary, config);

        ReflectionTestUtils.setField(
                storageService,
                "uploadDir",
                tempDir.toString()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "malicious.txt",
                "text/plain",
                "not an image".getBytes()
        );

        assertThrows(
                BadRequestException.class,
                () -> storageService.store(file)
        );
    }

    @Test
    @DisplayName("Ignore malicious original filename during local storage")
    void testOriginalFilenameCannotControlStoragePath(@TempDir Path tempDir) {
        CloudinaryConfig config = new CloudinaryConfig();
        Cloudinary cloudinary = config.cloudinary();

        FileStorageServiceImpl storageService =
                new FileStorageServiceImpl(cloudinary, config);

        ReflectionTestUtils.setField(
                storageService,
                "uploadDir",
                tempDir.toString()
        );

        MockMultipartFile file = new MockMultipartFile(
                "file",
                "../../outside-directory.jpg",
                "image/jpeg",
                "dummy image content".getBytes()
        );

        String resultPath = storageService.store(file);

        assertNotNull(resultPath);
        assertTrue(resultPath.startsWith("/uploads/"));
        assertTrue(resultPath.endsWith(".jpg"));

        String filename = resultPath.substring("/uploads/".length());

        Path storedFile = tempDir.resolve(filename).normalize();

        assertEquals(tempDir.toAbsolutePath().normalize(),
                storedFile.getParent());

        assertTrue(Files.exists(storedFile));
    }
}