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
public class SalesReportResponse {
    private long totalOrders;
    private BigDecimal totalRevenue;
    private long totalUnitsSold;
    private java.util.List<ProductSales> topProducts;

    /** Today/Weekly(ISO Monday-start)/Monthly/Yearly summaries, always computed as of "now" regardless of any date-range filter applied to the fields above. */
    private PeriodSummary today;
    private PeriodSummary weekly;
    private PeriodSummary monthly;
    private PeriodSummary yearly;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PeriodSummary {
        private long orders;
        private BigDecimal revenue;
        private long unitsSold;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProductSales {
        private Long productId;
        private String productName;
        private String unit;
        private long unitsSold;
        private BigDecimal revenue;
    }
}
