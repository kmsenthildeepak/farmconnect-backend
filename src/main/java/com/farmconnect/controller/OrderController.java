package com.farmconnect.controller;

import com.farmconnect.dto.request.OrderStatusUpdateRequest;
import com.farmconnect.dto.request.PlaceOrderRequest;
import com.farmconnect.dto.response.OrderResponse;
import com.farmconnect.service.impl.FarmerServiceImpl;
import com.farmconnect.service.impl.OrderServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderServiceImpl orderService;
    private final FarmerServiceImpl farmerService;

    // ---- Customer ----
    @PostMapping("/api/customer/orders")
    public ResponseEntity<OrderResponse> placeOrder(@Valid @RequestBody PlaceOrderRequest req) {
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.placeOrder(req));
    }

    @GetMapping("/api/customer/orders")
    public List<OrderResponse> myOrders() {
        return orderService.myOrders();
    }

    @GetMapping("/api/customer/orders/{id}")
    public OrderResponse getOrder(@PathVariable Long id) {
        return orderService.getOrder(id);
    }

    @PatchMapping("/api/customer/orders/{id}/cancel")
    public OrderResponse cancelOrder(@PathVariable Long id) {
        return orderService.cancelOrder(id);
    }

    // ---- Farmer ----
    @GetMapping("/api/farmer/orders")
    public List<OrderResponse> farmerOrders() {
        Long farmerId = farmerService.myProfile().getFarmerId();
        return orderService.farmerOrders(farmerId);
    }

    @PatchMapping("/api/farmer/orders/{id}/status")
    public OrderResponse updateStatus(@PathVariable Long id, @Valid @RequestBody OrderStatusUpdateRequest req) {
        return orderService.updateStatus(id, req.getStatus());
    }

    // ---- Admin ----
    @GetMapping("/api/admin/orders")
    public List<OrderResponse> allOrders() {
        return orderService.allOrders();
    }
}
