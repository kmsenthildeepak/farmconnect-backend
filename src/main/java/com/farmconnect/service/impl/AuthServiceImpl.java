package com.farmconnect.service.impl;

import com.farmconnect.dto.request.LoginRequest;
import com.farmconnect.dto.request.RegisterRequest;
import com.farmconnect.dto.response.AuthResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.entity.Notification;
import com.farmconnect.entity.User;
import com.farmconnect.exception.BadRequestException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.security.JwtService;
import com.farmconnect.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl {

    private final UserRepository userRepository;
    private final FarmerRepository farmerRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final NotificationServiceImpl notificationService;

    @Transactional
    public AuthResponse register(RegisterRequest req) {

        if (!req.getPassword().equals(req.getConfirmPassword())) {
            throw new BadRequestException("Passwords do not match");
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new BadRequestException(
                    "An account with this email already exists"
            );
        }

        User user = User.builder()
                .name(req.getName())
                .email(req.getEmail())
                .phone(req.getPhone())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(req.getRole())
                .address(req.getAddress())
                .city(req.getCity())
                .state(req.getState())
                .pincode(req.getPincode())
                .latitude(req.getLatitude())
                .longitude(req.getLongitude())
                .isVerified(req.getRole() != User.Role.FARMER)
                .isActive(true)
                .build();

        // Save once and keep the result effectively final.
        final User savedUser = userRepository.save(user);

        /*
         * FARMER REGISTRATION
         *
         * Create the farmer profile as PENDING.
         * Then notify every administrator.
         */
        if (req.getRole() == User.Role.FARMER) {

            Farmer farmer = Farmer.builder()
                    .user(savedUser)
                    .farmName(
                            req.getFarmName() != null
                                    ? req.getFarmName()
                                    : req.getName() + "'s Farm"
                    )
                    .farmAddress(
                            req.getFarmAddress() != null
                                    ? req.getFarmAddress()
                                    : req.getAddress()
                    )
                    .verificationStatus(
                            Farmer.VerificationStatus.PENDING
                    )
                    .build();

            farmerRepository.save(farmer);

            userRepository.findByRole(User.Role.ADMIN)
                    .forEach(admin ->
                            notificationService.notify(
                                    admin,
                                    "New Farmer Registration",
                                    savedUser.getName()
                                            + " has registered as a farmer "
                                            + "and is waiting for verification.",
                                    Notification.NotificationType.VERIFICATION
                            )
                    );
        }

        UserPrincipal principal = new UserPrincipal(savedUser);

        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(savedUser.getUserId())
                .name(savedUser.getName())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .isVerified(savedUser.getIsVerified())
                .build();
    }

    public AuthResponse login(LoginRequest req) {

        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        req.getEmail(),
                        req.getPassword()
                )
        );

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(
                        () -> new BadRequestException(
                                "Invalid email or password"
                        )
                );

        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new org.springframework.security.authentication.DisabledException(
                    "You were blocked by admin. You are requested to contact Admin for further process."
            );
        }

        if (user.getRole() == User.Role.FARMER) {
            Farmer farmer = farmerRepository.findByUser_UserId(user.getUserId())
                    .orElseThrow(() -> new BadRequestException(
                            "Farmer profile not found. Please contact the admin."));

            if (farmer.getVerificationStatus() == Farmer.VerificationStatus.PENDING) {
                throw new BadRequestException(
                        "You may login once you have been verified by the admin."
                );
            }
            if (farmer.getVerificationStatus() == Farmer.VerificationStatus.REJECTED) {
                throw new BadRequestException(
                        "Your account has been rejected. Please contact the admin."
                );
            }
        }

        UserPrincipal principal = new UserPrincipal(user);

        String token = jwtService.generateToken(principal);

        return AuthResponse.builder()
                .token(token)
                .userId(user.getUserId())
                .name(user.getName())
                .email(user.getEmail())
                .role(user.getRole())
                .isVerified(user.getIsVerified())
                .build();
    }
}
