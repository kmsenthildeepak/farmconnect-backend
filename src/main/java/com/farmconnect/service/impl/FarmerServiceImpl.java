package com.farmconnect.service.impl;

import com.farmconnect.dto.response.FarmerResponse;
import com.farmconnect.entity.Farmer;
import com.farmconnect.exception.ResourceNotFoundException;
import com.farmconnect.repository.FarmerRepository;
import com.farmconnect.util.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class FarmerServiceImpl {

    private final FarmerRepository farmerRepository;

    @Transactional(readOnly = true)
public FarmerResponse getFarmer(Long farmerId) {
    return toResponse(farmerRepository.findById(farmerId)
            .orElseThrow(() -> new ResourceNotFoundException("Farmer not found")));
}

    @Transactional(readOnly = true)
    public FarmerResponse getFarmerByEmail(String email) {
        return toResponse(farmerRepository.findByUser_EmailIgnoreCase(email)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found")));
    }

@Transactional(readOnly = true)
public FarmerResponse myProfile() {
    Farmer farmer = farmerRepository.findByUser_UserId(SecurityUtil.currentUserId())
            .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));
    return toResponse(farmer);
}

    @Transactional
    public FarmerResponse updateProfile(String farmName, String farmAddress, String farmLocation,
                                         Integer experience, String description) {
        Farmer farmer = farmerRepository.findByUser_UserId(SecurityUtil.currentUserId())
                .orElseThrow(() -> new ResourceNotFoundException("Farmer profile not found"));
        if (farmName != null) farmer.setFarmName(farmName);
        if (farmAddress != null) farmer.setFarmAddress(farmAddress);
        if (farmLocation != null) farmer.setFarmLocation(farmLocation);
        if (experience != null) farmer.setExperience(experience);
        if (description != null) farmer.setDescription(description);
        return toResponse(farmerRepository.save(farmer));
    }

    @Transactional(readOnly = true)
public List<FarmerResponse> pendingVerifications() {
    return farmerRepository.findByVerificationStatus(Farmer.VerificationStatus.PENDING)
            .stream()
            .map(this::toResponse)
            .toList();
}

    @Transactional(readOnly = true)
    public List<FarmerResponse> allFarmers() {
        return farmerRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }
    @Transactional
    public FarmerResponse setVerificationStatus(Long farmerId, Farmer.VerificationStatus status) {
        Farmer farmer = farmerRepository.findById(farmerId)
                .orElseThrow(() -> new ResourceNotFoundException("Farmer not found"));
        farmer.setVerificationStatus(status);
        farmer.getUser().setIsVerified(status == Farmer.VerificationStatus.VERIFIED);
        return toResponse(farmerRepository.save(farmer));
    }

    private FarmerResponse toResponse(Farmer f) {
        return FarmerResponse.builder()
                .farmerId(f.getFarmerId())
                .userId(f.getUser().getUserId())
                .farmerName(f.getUser().getName())
                .email(f.getUser().getEmail())
                .farmName(f.getFarmName())
                .farmAddress(f.getFarmAddress())
                .farmLocation(f.getFarmLocation())
                .experience(f.getExperience())
                .description(f.getDescription())
                .verificationStatus(f.getVerificationStatus())
                .profileImage(f.getUser().getProfileImage())
                .phone(f.getUser().getPhone())
                .isActive(f.getUser().getIsActive())
                .build();
    }
}
