package com.farmconnect.dto.request;

import com.farmconnect.entity.Farmer;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class FarmerVerificationRequest {
    @NotNull
    private Farmer.VerificationStatus status;
}
