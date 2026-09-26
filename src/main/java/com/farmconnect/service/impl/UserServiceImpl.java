package com.farmconnect.service.impl;

import com.farmconnect.dto.request.UpdateAddressRequest;
import com.farmconnect.dto.response.UserResponse;
import com.farmconnect.entity.User;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.UserRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse myProfile() {
        return toResponse(currentUser());
    }

    @Transactional
    public UserResponse updateMyAddress(UpdateAddressRequest req) {
        User user = currentUser();
        user.setAddress(req.getAddress());
        user.setCity(req.getCity());
        user.setState(req.getState());
        user.setPincode(req.getPincode());
        return toResponse(userRepository.save(user));
    }

    @Transactional
    public UserResponse updateMyContact(com.farmconnect.dto.request.UpdateContactRequest req) {
        User user = currentUser();
        user.setName(req.getName());
        user.setPhone(req.getPhone());
        return toResponse(userRepository.save(user));
    }

    private User currentUser() {
        return userRepository.findById(SecurityUtil.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    private UserResponse toResponse(User u) {
        return UserResponse.builder()
                .userId(u.getUserId())
                .name(u.getName())
                .email(u.getEmail())
                .phone(u.getPhone())
                .role(u.getRole())
                .profileImage(u.getProfileImage())
                .address(u.getAddress())
                .city(u.getCity())
                .state(u.getState())
                .pincode(u.getPincode())
                .isVerified(u.getIsVerified())
                .isActive(u.getIsActive())
                .createdAt(u.getCreatedAt())
                .build();
    }
}
