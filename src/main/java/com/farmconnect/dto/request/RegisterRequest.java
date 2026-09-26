package com.farmconnect.dto.request;

import com.farmconnect.entity.User;
import jakarta.validation.constraints.*;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Enter a valid email")
    private String email;

    @NotBlank(message = "Phone is required")
    @Pattern(regexp = "^[0-9]{10}$", message = "Phone must be 10 digits")
    private String phone;

    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Confirm your password")
    private String confirmPassword;

    @NotNull(message = "Role is required")
    private User.Role role;

    private String address;
    private String city;
    private String state;
    private String pincode;
    private Double latitude;
    private Double longitude;

    // Only used when role == FARMER
    private String farmName;
    private String farmAddress;
}
