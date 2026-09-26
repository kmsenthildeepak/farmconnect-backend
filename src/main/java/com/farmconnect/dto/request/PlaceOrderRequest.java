package com.farmconnect.dto.request;

import com.farmconnect.entity.Order;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class PlaceOrderRequest {
    @NotBlank(message = "Delivery address is required")
    private String shippingAddress;

    @NotBlank(message = "A contact mobile number is required")
    @Pattern(regexp = "^[6-9]\\d{9}$", message = "Enter a valid 10-digit mobile number")
    private String contactPhone;

    @NotNull
    private Order.PaymentMethod paymentMethod;
}
