package com.farmconnect.controller;

import com.farmconnect.dto.request.ForgotPasswordRequest;
import com.farmconnect.dto.request.LoginRequest;
import com.farmconnect.dto.request.RegisterRequest;
import com.farmconnect.dto.request.ResetPasswordRequest;
import com.farmconnect.dto.response.AuthResponse;
import com.farmconnect.dto.response.MessageResponse;
import com.farmconnect.service.PasswordResetService;
import com.farmconnect.service.impl.AuthServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthServiceImpl authService;
    private final PasswordResetService passwordResetService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(req));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest req) {
        return ResponseEntity.ok(authService.login(req));
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<MessageResponse> forgotPassword(@Valid @RequestBody ForgotPasswordRequest req) {
        return ResponseEntity.ok(passwordResetService.forgotPassword(req));
    }

    @PostMapping("/reset-password")
    public ResponseEntity<MessageResponse> resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        return ResponseEntity.ok(passwordResetService.resetPassword(req));
    }
}
