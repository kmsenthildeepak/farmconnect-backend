package com.farmconnect.dto.response;

import com.farmconnect.entity.Order;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private Long orderId;
    private Long customerId;
    private String customerName;
    private String customerPhone;
    private BigDecimal totalAmount;
    private String shippingAddress;
    private Order.PaymentMethod paymentMethod;
    private Order.PaymentStatus paymentStatus;
    private Order.OrderStatus orderStatus;
    private List<OrderItemResponse> items;
    private LocalDateTime createdAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OrderItemResponse {
        private Long orderItemId;
        private Long productId;
        private String productName;
        private String imageUrl;
        private Long farmerId;
        private String farmName;
        private Integer quantity;
        private BigDecimal price;
        private BigDecimal subtotal;
    }
}
