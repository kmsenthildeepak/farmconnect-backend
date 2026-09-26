package com.farmconnect.controller;

import com.farmconnect.dto.request.CartItemRequest;
import com.farmconnect.dto.response.CartResponse;
import com.farmconnect.service.impl.CartServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/customer/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartServiceImpl cartService;

    @GetMapping
    public CartResponse getCart() {
        return cartService.getCart();
    }

    @PostMapping("/items")
    public CartResponse addItem(@Valid @RequestBody CartItemRequest req) {
        return cartService.addItem(req);
    }

    @PutMapping("/items/{cartItemId}")
    public CartResponse updateItem(@PathVariable Long cartItemId, @RequestParam int quantity) {
        return cartService.updateItemQuantity(cartItemId, quantity);
    }

    @DeleteMapping("/items/{cartItemId}")
    public CartResponse removeItem(@PathVariable Long cartItemId) {
        return cartService.removeItem(cartItemId);
    }

    @DeleteMapping
    public ResponseEntity<Void> clear() {
        cartService.clearCart();
        return ResponseEntity.noContent().build();
    }
}
