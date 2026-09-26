package com.farmconnect.controller;

import com.farmconnect.dto.request.FarmerVerificationRequest;
import com.farmconnect.dto.response.DashboardResponse;
import com.farmconnect.dto.response.FarmerResponse;
import com.farmconnect.dto.response.ProductResponse;
import com.farmconnect.dto.response.UserResponse;
import com.farmconnect.entity.User;
import com.farmconnect.service.impl.AdminServiceImpl;
import com.farmconnect.service.impl.FarmerServiceImpl;
import com.farmconnect.service.impl.ProductServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminServiceImpl adminService;
    private final FarmerServiceImpl farmerService;
    private final ProductServiceImpl productService;

    @GetMapping("/dashboard")
    public DashboardResponse dashboard() {
        return adminService.getDashboard();
    }

    @GetMapping("/users")
    public List<UserResponse> allUsers() {
        return adminService.allUsers().stream().map(this::toUserResponse).toList();
    }

    @GetMapping("/customers")
    public List<UserResponse> allCustomers() {
        return adminService.allCustomers().stream().map(this::toUserResponse).toList();
    }

    @GetMapping("/farmers")
    public List<FarmerResponse> allFarmers() {
        return farmerService.allFarmers();
    }

    /**
     * Admin farmer search by exactly one of farmerId or email. Read-only,
     * ADMIN-gated (see SecurityConfig's /api/admin/** rule) - a farmer or
     * customer token cannot reach this endpoint at all.
     */
    @GetMapping("/farmers/search")
    public FarmerResponse searchFarmer(
            @RequestParam(required = false) Long farmerId,
            @RequestParam(required = false) String email) {
        if (farmerId != null) {
            return farmerService.getFarmer(farmerId);
        }
        if (email != null && !email.isBlank()) {
            return farmerService.getFarmerByEmail(email);
        }
        throw new com.farmconnect.exception.BadRequestException("Provide either farmerId or email");
    }

    @GetMapping("/farmers/{farmerId}/products")
    public List<ProductResponse> farmerProducts(@PathVariable Long farmerId) {
        return productService.adminProductsByFarmer(farmerId);
    }

    @GetMapping("/products")
    public List<ProductResponse> allProducts() {
        return productService.adminAllProducts();
    }

    @PatchMapping("/users/{id}/deactivate")
    public void deactivate(@PathVariable Long id) {
        adminService.deactivateUser(id);
    }

    @PatchMapping("/users/{id}/activate")
    public void activate(@PathVariable Long id) {
        adminService.activateUser(id);
    }

    /**
     * Blocks a farmer (id = farmerId, not userId): disables their login and
     * hides their products. See AdminServiceImpl#blockFarmer for why this
     * is a block rather than a hard delete.
     */
    @DeleteMapping("/farmers/{farmerId}")
    public void blockFarmer(@PathVariable Long farmerId) {
        adminService.blockFarmer(farmerId);
    }

    @PutMapping("/farmers/{farmerId}/unblock")
    public void unblockFarmer(@PathVariable Long farmerId) {
        adminService.unblockFarmer(farmerId);
    }

    /**
     * Blocks a customer (id = userId): disables their login. See
     * AdminServiceImpl#blockCustomer for why existing reviews/orders are
     * left in place.
     */
    @DeleteMapping("/customers/{userId}")
    public void blockCustomer(@PathVariable Long userId) {
        adminService.blockCustomer(userId);
    }

    @PutMapping("/customers/{userId}/unblock")
    public void unblockCustomer(@PathVariable Long userId) {
        adminService.unblockCustomer(userId);
    }

    @GetMapping("/farmers/pending")
    public List<FarmerResponse> pendingFarmers() {
        return farmerService.pendingVerifications();
    }

    @PatchMapping("/farmers/{id}/verify")
    public FarmerResponse verifyFarmer(@PathVariable Long id, @Valid @RequestBody FarmerVerificationRequest req) {
        return farmerService.setVerificationStatus(id, req.getStatus());
    }

    private UserResponse toUserResponse(User u) {
        return UserResponse.builder()
                .userId(u.getUserId()).name(u.getName()).email(u.getEmail()).phone(u.getPhone())
                .role(u.getRole()).profileImage(u.getProfileImage()).address(u.getAddress())
                .city(u.getCity()).state(u.getState()).pincode(u.getPincode())
                .isVerified(u.getIsVerified()).isActive(u.getIsActive()).createdAt(u.getCreatedAt())
                .build();
    }
}
