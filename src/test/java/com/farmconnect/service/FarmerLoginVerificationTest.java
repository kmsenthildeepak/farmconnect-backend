package com.farmconnect.service;

import com.farmconnect.dto.request.LoginRequest;
import com.farmconnect.dto.response.AuthResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.JwtService;
import com.farmconnect.security.UserPrincipal;
import com.farmconnect.service.impl.AuthServiceImpl;
import com.farmconnect.service.impl.NotificationServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class FarmerLoginVerificationTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private FarmerRepository farmerRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private AuthenticationManager authenticationManager;

    @Mock
    private JwtService jwtService;

    @Mock
    private NotificationServiceImpl notificationService;

    @InjectMocks
    private AuthServiceImpl authService;

    private User farmerUser;
    private Farmer pendingFarmer;
    private Farmer verifiedFarmer;
    private LoginRequest loginRequest;

    @BeforeEach
    void setUp() {
        farmerUser = User.builder()
                .userId(101L)
                .name("Test Farmer")
                .email("farmer@farmconnect.com")
                .password("encoded_pass")
                .role(User.Role.FARMER)
                .isActive(true)
                .isVerified(false)
                .build();

        pendingFarmer = Farmer.builder()
                .farmerId(1L)
                .user(farmerUser)
                .farmName("Green Acres")
                .verificationStatus(Farmer.VerificationStatus.PENDING)
                .build();

        verifiedFarmer = Farmer.builder()
                .farmerId(1L)
                .user(farmerUser)
                .farmName("Green Acres")
                .verificationStatus(Farmer.VerificationStatus.VERIFIED)
                .build();

        loginRequest = new LoginRequest();
        loginRequest.setEmail("farmer@farmconnect.com");
        loginRequest.setPassword("plain_pass");
    }

    @Test
    @DisplayName("Unverified farmer with correct credentials receives exact verification message")
    void testUnverifiedFarmerLoginMessage() {
        when(userRepository.findByEmail("farmer@farmconnect.com")).thenReturn(Optional.of(farmerUser));
        when(farmerRepository.findByUser_UserId(101L)).thenReturn(Optional.of(pendingFarmer));

        BadRequestException ex = assertThrows(
                BadRequestException.class,
                () -> authService.login(loginRequest)
        );

        assertEquals("You may login once you have been verified by the admin.", ex.getMessage());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    @DisplayName("Invalid credentials throw BadCredentialsException before farmer verification check")
    void testInvalidCredentialsThrowsBadCredentials() {
        doThrow(new BadCredentialsException("Bad credentials"))
                .when(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));

        assertThrows(
                BadCredentialsException.class,
                () -> authService.login(loginRequest)
        );

        verify(userRepository, never()).findByEmail(any());
        verify(farmerRepository, never()).findByUser_UserId(any());
    }

    @Test
    @DisplayName("Verified farmer with correct credentials successfully logs in")
    void testVerifiedFarmerLoginSuccess() {
        when(userRepository.findByEmail("farmer@farmconnect.com")).thenReturn(Optional.of(farmerUser));
        when(farmerRepository.findByUser_UserId(101L)).thenReturn(Optional.of(verifiedFarmer));
        when(jwtService.generateToken(any(UserPrincipal.class))).thenReturn("mock_jwt_token");

        AuthResponse response = authService.login(loginRequest);

        assertNotNull(response);
        assertEquals("mock_jwt_token", response.getToken());
        assertEquals("farmer@farmconnect.com", response.getEmail());
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }
}
