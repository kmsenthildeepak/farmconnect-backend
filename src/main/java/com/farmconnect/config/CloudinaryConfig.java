package com.farmconnect.config;

import com.cloudinary.Cloudinary;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
@Getter
public class CloudinaryConfig {

    @Value("${cloudinary.cloud-name:}")
    private String cloudName;

    @Value("${cloudinary.api-key:}")
    private String apiKey;

    @Value("${cloudinary.api-secret:}")
    private String apiSecret;

    @Bean
    public Cloudinary cloudinary() {
        Map<String, Object> config = new HashMap<>();
        if (cloudName != null && !cloudName.isBlank()) {
            config.put("cloud_name", cloudName.trim());
        }
        if (apiKey != null && !apiKey.isBlank()) {
            config.put("api_key", apiKey.trim());
        }
        if (apiSecret != null && !apiSecret.isBlank()) {
            config.put("api_secret", apiSecret.trim());
        }
        config.put("secure", true);
        return new Cloudinary(config);
    }

    public boolean isConfigured() {
        return cloudName != null && !cloudName.isBlank()
                && apiKey != null && !apiKey.isBlank()
                && apiSecret != null && !apiSecret.isBlank();
    }
}
