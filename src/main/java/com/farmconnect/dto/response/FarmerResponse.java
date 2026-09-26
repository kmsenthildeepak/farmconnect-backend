package com.farmconnect.dto.response;

import com.farmconnect.entity.Farmer;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FarmerResponse {
    private Long farmerId;
    private Long userId;
    private String farmerName;
    private String email;
    private String farmName;
    private String farmAddress;
    private String farmLocation;
    private Integer experience;
    private String description;
    private Farmer.VerificationStatus verificationStatus;
    private String profileImage;
    private String phone;
    private Boolean isActive;
}
