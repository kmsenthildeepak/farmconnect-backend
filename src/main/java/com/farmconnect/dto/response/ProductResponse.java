package com.farmconnect.dto.response;

import com.farmconnect.entity.Product;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductResponse {
    private Long productId;
    private Long farmerId;
    private String farmName;
    private String farmerCity;
    private String productName;
    private String description;
    private Product.Category category;
    private BigDecimal price;
    private Product.Unit unit;
    private Integer quantity;
    private String imageUrl;
    private Boolean isOrganic;
    private Boolean availability;
    private Double averageRating;
    private Integer reviewCount;
    private LocalDateTime createdAt;
}
