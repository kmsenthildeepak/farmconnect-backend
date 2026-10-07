package com.farmconnect.service;

public interface EmailService {
    void sendPasswordResetEmail(String toEmail, String userName, String resetUrl);
    String buildResetPasswordUrl(String token);
}
