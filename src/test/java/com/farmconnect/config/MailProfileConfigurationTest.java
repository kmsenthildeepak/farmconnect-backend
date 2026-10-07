package com.farmconnect.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.InputStream;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class MailProfileConfigurationTest {

    @Test
    @DisplayName("Verify application.properties has default mailpit profile and shared properties")
    void testMainProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application.properties")) {
            assertNotNull(is, "application.properties should exist");
            props.load(is);
        }
        assertEquals("${SPRING_PROFILES_ACTIVE:mailpit}", props.getProperty("spring.profiles.active"));
        assertEquals("FarmConnect", props.getProperty("app.mail.sender-name"));
        assertEquals("${RESET_BASE_URL:http://localhost:8080}", props.getProperty("app.mail.reset-base-url"));
        assertEquals("${app.mail.reset-base-url}/reset-password", props.getProperty("app.reset-password.base-url"));
        assertEquals("30", props.getProperty("app.reset-password.expiration-minutes"));
    }

    @Test
    @DisplayName("Verify application-mailpit.properties configuration")
    void testMailpitProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application-mailpit.properties")) {
            assertNotNull(is, "application-mailpit.properties should exist");
            props.load(is);
        }
        assertEquals("localhost", props.getProperty("spring.mail.host"));
        assertEquals("1025", props.getProperty("spring.mail.port"));
        assertEquals("false", props.getProperty("spring.mail.properties.mail.smtp.auth"));
        assertEquals("false", props.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        assertEquals("noreply@farmconnect.com", props.getProperty("app.mail.sender-email"));
    }

    @Test
    @DisplayName("Verify application-gmail.properties configuration syntax and placeholders")
    void testGmailProperties() throws Exception {
        Properties props = new Properties();
        try (InputStream is = getClass().getResourceAsStream("/application-gmail.properties")) {
            assertNotNull(is, "application-gmail.properties should exist");
            props.load(is);
        }
        assertEquals("smtp.gmail.com", props.getProperty("spring.mail.host"));
        assertEquals("587", props.getProperty("spring.mail.port"));
        assertEquals("true", props.getProperty("spring.mail.properties.mail.smtp.auth"));
        assertEquals("true", props.getProperty("spring.mail.properties.mail.smtp.starttls.enable"));
        assertEquals("true", props.getProperty("spring.mail.properties.mail.smtp.starttls.required"));
        assertEquals("${MAIL_USERNAME:}", props.getProperty("spring.mail.username"));
        assertEquals("${MAIL_PASSWORD:}", props.getProperty("spring.mail.password"));
        assertEquals("${MAIL_USERNAME:noreply@farmconnect.com}", props.getProperty("app.mail.sender-email"));
    }

    @Test
    @DisplayName("Verify EmailServiceImpl constructs reset URL properly with and without trailing slash")
    void testResetPasswordUrlConstruction() {
        com.farmconnect.service.impl.EmailServiceImpl emailService =
                new com.farmconnect.service.impl.EmailServiceImpl(null);

        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "resetBaseUrl", "http://localhost:8080");
        assertEquals("http://localhost:8080/reset-password?token=sampleToken123", emailService.buildResetPasswordUrl("sampleToken123"));

        org.springframework.test.util.ReflectionTestUtils.setField(emailService, "resetBaseUrl", "http://custom-host:8080/");
        assertEquals("http://custom-host:8080/reset-password?token=sampleToken123", emailService.buildResetPasswordUrl("sampleToken123"));
    }
}
