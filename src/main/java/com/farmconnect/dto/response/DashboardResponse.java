package com.farmconnect.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {
    private long totalUsers;
    private long totalFarmers;
    private long totalCustomers;
    private long totalProducts;
    private long totalOrders;
    private long pendingFarmerVerifications;
    private BigDecimal totalRevenue;
}
