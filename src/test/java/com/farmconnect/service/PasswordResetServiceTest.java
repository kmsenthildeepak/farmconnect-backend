package com.farmconnect.service;

import com.farmconnect.dto.request.ForgotPasswordRequest;
import com.farmconnect.dto.request.ResetPasswordRequest;
import com.farmconnect.dto.response.MessageResponse;
import com.farmconnect.entity.PasswordResetToken;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.repository.PasswordResetTokenRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.service.impl.PasswordResetServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class PasswordResetServiceTest {

    private static final String GENERIC_RESPONSE =
            "If an account exists with this email, a password reset link has been sent.";

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Mock
    private EmailService emailService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private PasswordResetServiceImpl passwordResetService;

    private User testUser;

    @BeforeEach
    public void setup() {
        ReflectionTestUtils.setField(passwordResetService, "resetPasswordBaseUrl", "http://localhost:8080/reset-password");
        ReflectionTestUtils.setField(passwordResetService, "expirationMinutes", 30);

        testUser = User.builder()
                .userId(1L)
                .name("Chandru M")
                .email("chandruazhagaa@farmconnect.com")
                .password("$2a$10$oldHashedPassword")
                .role(User.Role.CUSTOMER)
                .isActive(true)
                .build();
    }

    private String computeSha256(String raw) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(raw.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(hash);
    }

    @Test
    public void testForgotPassword_ExistingUser_GeneratesTokenAndSendsEmail() {
        when(userRepository.findByEmail("chandruazhagaa@farmconnect.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findByUserAndUsedFalse(testUser)).thenReturn(List.of());

        ForgotPasswordRequest req = new ForgotPasswordRequest("chandruazhagaa@farmconnect.com");
        MessageResponse response = passwordResetService.forgotPassword(req);

        assertEquals(GENERIC_RESPONSE, response.getMessage());

        // Verify token saved with SHA-256 hash (64 hex characters)
        ArgumentCaptor<PasswordResetToken> tokenCaptor = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(tokenCaptor.capture());
        PasswordResetToken savedToken = tokenCaptor.getValue();

        assertNotNull(savedToken.getTokenHash());
        assertEquals(64, savedToken.getTokenHash().length());
        assertFalse(savedToken.getUsed());
        assertTrue(savedToken.getExpiresAt().isAfter(LocalDateTime.now()));

        // Verify email sent with reset link containing raw token
        verify(emailService).sendPasswordResetEmail(eq("chandruazhagaa@farmconnect.com"), eq("Chandru M"), any(String.class));
    }

    @Test
    public void testForgotPassword_UnknownUser_ReturnsSameGenericMessageWithoutSendingEmail() {
        when(userRepository.findByEmail("unknown@farmconnect.com")).thenReturn(Optional.empty());

        ForgotPasswordRequest req = new ForgotPasswordRequest("unknown@farmconnect.com");
        MessageResponse response = passwordResetService.forgotPassword(req);

        assertEquals(GENERIC_RESPONSE, response.getMessage());
        verify(passwordResetTokenRepository, never()).save(any());
        verify(emailService, never()).sendPasswordResetEmail(any(), any(), any());
    }

    @Test
    public void testForgotPassword_InvalidatesPriorActiveTokens() {
        PasswordResetToken oldToken = PasswordResetToken.builder()
                .id(10L)
                .user(testUser)
                .tokenHash("oldhash")
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(20))
                .build();

        when(userRepository.findByEmail("chandruazhagaa@farmconnect.com")).thenReturn(Optional.of(testUser));
        when(passwordResetTokenRepository.findByUserAndUsedFalse(testUser)).thenReturn(List.of(oldToken));

        ForgotPasswordRequest req = new ForgotPasswordRequest("chandruazhagaa@farmconnect.com");
        passwordResetService.forgotPassword(req);

        assertTrue(oldToken.getUsed(), "Old token must be marked as used");
        verify(passwordResetTokenRepository).saveAll(List.of(oldToken));
    }

    @Test
    public void testResetPassword_ValidToken_SuccessfullyUpdatesPasswordAndMarksTokenUsed() throws Exception {
        String rawToken = "valid-raw-test-token-1234567890";
        String tokenHash = computeSha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash(tokenHash)
                .used(false)
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));
        when(passwordEncoder.encode("newPassword123")).thenReturn("$2a$10$newHashedPassword");

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "newPassword123");
        MessageResponse response = passwordResetService.resetPassword(req);

        assertEquals("Password has been reset successfully. You can now log in with your new password.", response.getMessage());
        assertEquals("$2a$10$newHashedPassword", testUser.getPassword());
        assertTrue(resetToken.getUsed(), "Token must be invalidated after use");
        verify(userRepository).save(testUser);
        verify(passwordResetTokenRepository).save(resetToken);
    }

    @Test
    public void testResetPassword_ExpiredToken_ThrowsBadRequestException() throws Exception {
        String rawToken = "expired-raw-token-1234567890";
        String tokenHash = computeSha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash(tokenHash)
                .used(false)
                .expiresAt(LocalDateTime.now().minusMinutes(5)) // Expired 5 mins ago
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "newPassword123");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(req));

        assertEquals("This password reset link has expired", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testResetPassword_AlreadyUsedToken_ThrowsBadRequestException() throws Exception {
        String rawToken = "used-raw-token-1234567890";
        String tokenHash = computeSha256(rawToken);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(1L)
                .user(testUser)
                .tokenHash(tokenHash)
                .used(true) // Already used
                .expiresAt(LocalDateTime.now().plusMinutes(15))
                .build();

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.of(resetToken));

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "newPassword123");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(req));

        assertEquals("This password reset link has already been used", ex.getMessage());
        verify(userRepository, never()).save(any());
    }

    @Test
    public void testResetPassword_InvalidToken_ThrowsBadRequestException() throws Exception {
        String rawToken = "non-existent-token-1234567890";
        String tokenHash = computeSha256(rawToken);

        when(passwordResetTokenRepository.findByTokenHash(tokenHash)).thenReturn(Optional.empty());

        ResetPasswordRequest req = new ResetPasswordRequest(rawToken, "newPassword123");
        BadRequestException ex = assertThrows(BadRequestException.class, () -> passwordResetService.resetPassword(req));

        assertEquals("Invalid or expired password reset link", ex.getMessage());
        verify(userRepository, never()).save(any());
    }
}
