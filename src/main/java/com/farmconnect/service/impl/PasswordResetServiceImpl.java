package com.farmconnect.service.impl;

import com.farmconnect.dto.request.ForgotPasswordRequest;
import com.farmconnect.dto.request.ResetPasswordRequest;
import com.farmconnect.dto.response.MessageResponse;
import com.farmconnect.entity.PasswordResetToken;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.repository.PasswordResetTokenRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.service.EmailService;
import com.farmconnect.service.PasswordResetService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final String GENERIC_FORGOT_PASSWORD_MSG =
            "If an account exists with this email, a password reset link has been sent.";

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;
    private final SecureRandom secureRandom = new SecureRandom();

    @Value("${app.mail.reset-base-url:${RESET_BASE_URL:http://localhost:8080}}")
    private String resetBaseUrl;

    @Value("${app.reset-password.base-url:}")
    private String resetPasswordBaseUrl;

    @Value("${app.reset-password.expiration-minutes:30}")
    private int expirationMinutes;

    @Override
    @Transactional
    public MessageResponse forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        Optional<User> userOpt = userRepository.findByEmail(email);

        if (userOpt.isPresent()) {
            User user = userOpt.get();

            // 1. Invalidate any existing unused reset tokens for this user
            List<PasswordResetToken> existingTokens =
                    passwordResetTokenRepository.findByUserAndUsedFalse(user);
            for (PasswordResetToken token : existingTokens) {
                token.setUsed(true);
            }
            passwordResetTokenRepository.saveAll(existingTokens);

            // 2. Generate cryptographically secure random token (32 bytes = 256 bits)
            byte[] randomBytes = new byte[32];
            secureRandom.nextBytes(randomBytes);
            String rawToken = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);

            // 3. Hash token with SHA-256 for secure database storage
            String tokenHash = hashToken(rawToken);

            // 4. Save token record
            LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(expirationMinutes);
            PasswordResetToken resetToken = PasswordResetToken.builder()
                    .user(user)
                    .tokenHash(tokenHash)
                    .expiresAt(expiresAt)
                    .used(false)
                    .build();
            passwordResetTokenRepository.save(resetToken);

            // 5. Send email (raw token is sent in email URL only, never logged)
            String resetUrl;
            if (resetPasswordBaseUrl != null && !resetPasswordBaseUrl.isBlank()) {
                String base = resetPasswordBaseUrl.trim();
                resetUrl = base + (base.contains("?") ? "&token=" : "?token=") + rawToken;
            } else {
                String base = (resetBaseUrl != null && !resetBaseUrl.isBlank()) ? resetBaseUrl.trim() : "http://localhost:8080";
                if (base.endsWith("/")) {
                    base = base.substring(0, base.length() - 1);
                }
                resetUrl = base + "/reset-password?token=" + rawToken;
            }
            emailService.sendPasswordResetEmail(user.getEmail(), user.getName(), resetUrl);
            log.info("Password reset initiated for user id {}", user.getUserId());
        } else {
            log.info("Password reset requested for non-existent email: {}", email);
        }

        // Return same generic message for existing or unknown email to prevent enumeration
        return new MessageResponse(GENERIC_FORGOT_PASSWORD_MSG);
    }

    @Override
    @Transactional
    public MessageResponse resetPassword(ResetPasswordRequest request) {
        String rawToken = request.getToken().trim();
        String tokenHash = hashToken(rawToken);

        PasswordResetToken resetToken = passwordResetTokenRepository.findByTokenHash(tokenHash)
                .orElseThrow(() -> new BadRequestException("Invalid or expired password reset link"));

        if (Boolean.TRUE.equals(resetToken.getUsed())) {
            throw new BadRequestException("This password reset link has already been used");
        }

        if (resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("This password reset link has expired");
        }

        User user = resetToken.getUser();
        if (user == null) {
            throw new BadRequestException("Invalid or expired password reset link");
        }

        // Hash new password using existing password encoder (BCrypt)
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Invalidate token
        resetToken.setUsed(true);
        passwordResetTokenRepository.save(resetToken);

        log.info("Password successfully reset for user id {}", user.getUserId());
        return new MessageResponse("Password has been reset successfully. You can now log in with your new password.");
    }

    private String hashToken(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(rawToken.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 algorithm not available", e);
        }
    }
}
