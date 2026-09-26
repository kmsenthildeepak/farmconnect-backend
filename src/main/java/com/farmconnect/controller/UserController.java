package com.farmconnect.controller;

import com.farmconnect.dto.request.UpdateAddressRequest;
import com.farmconnect.dto.response.UserResponse;
import com.farmconnect.service.impl.UserServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * Self-service endpoints for the currently authenticated user (customer or
 * farmer). Used by the Android "My Addresses" / "Profile" screens.
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserServiceImpl userService;

    @GetMapping("/me")
    public UserResponse myProfile() {
        return userService.myProfile();
    }

    @PutMapping("/me/address")
    public UserResponse updateMyAddress(@Valid @RequestBody UpdateAddressRequest req) {
        return userService.updateMyAddress(req);
    }

    @PutMapping("/me/contact")
    public UserResponse updateMyContact(@Valid @RequestBody com.farmconnect.dto.request.UpdateContactRequest req) {
        return userService.updateMyContact(req);
    }
}
