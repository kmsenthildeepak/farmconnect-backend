package com.farmconnect.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String userName, String resetUrl);
    default String buildResetPasswordUrl(String token) {
        return "http://localhost:8080/reset-password?token=" + token;
    }
}
