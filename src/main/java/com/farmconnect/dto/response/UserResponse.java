package com.farmconnect.dto.response;

import com.farmconnect.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserResponse {
    private Long userId;
    private String name;
    private String email;
    private String phone;
    private User.Role role;
    private String profileImage;
    private String address;
    private String city;
    private String state;
    private String pincode;
    private Boolean isVerified;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
