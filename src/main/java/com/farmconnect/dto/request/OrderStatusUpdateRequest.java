package com.farmconnect.dto.request;

import com.farmconnect.entity.Order;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull
    private Order.OrderStatus status;
}
