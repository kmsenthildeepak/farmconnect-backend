package com.farmconnect.controller;

import com.farmconnect.dto.response.FarmerResponse;
import com.farmconnect.service.impl.FarmerServiceImpl;
import com.farmconnect.service.impl.SalesReportServiceImpl;
import com.farmconnect.dto.response.SalesReportResponse;
import com.farmconnect.service.impl.FileStorageServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequiredArgsConstructor
public class FarmerController {

    private final FarmerServiceImpl farmerService;
    private final SalesReportServiceImpl salesReportService;
    private final FileStorageServiceImpl fileStorageService;

    @GetMapping("/api/farmers/{id}")
    public FarmerResponse getFarmer(@PathVariable Long id) {
        return farmerService.getFarmer(id);
    }

    @GetMapping("/api/farmer/sales-report")
    public SalesReportResponse salesReport(
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate startDate,
            @RequestParam(required = false)
            @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
            java.time.LocalDate endDate
    ) {
        return salesReportService.mySalesReport(startDate, endDate);
    }

    @GetMapping("/api/farmer/profile")
    public FarmerResponse myProfile() {
        return farmerService.myProfile();
    }

    @PutMapping("/api/farmer/profile")
    public FarmerResponse updateProfile(@RequestBody UpdateFarmerProfileRequest req) {
        return farmerService.updateProfile(req.farmName(), req.farmAddress(), req.farmLocation(),
                req.experience(), req.description());
    }

    @PostMapping("/api/farmer/profile/image")
    public String uploadProfileImage(@RequestParam MultipartFile file) {
        return fileStorageService.store(file);
    }

    public record UpdateFarmerProfileRequest(String farmName, String farmAddress, String farmLocation,
                                              Integer experience, String description) {}
}
