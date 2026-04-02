package com.example.ecommerce_backend.controller;

import com.example.ecommerce_backend.dto.request.CartRequest;
import com.example.ecommerce_backend.dto.response.CartResponse;
import com.example.ecommerce_backend.entity.User;
import com.example.ecommerce_backend.service.CartService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/cart")
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;

    // GET /api/cart — returns the authenticated user's cart only
    @GetMapping
    public ResponseEntity<List<CartResponse>> getCart(
            @AuthenticationPrincipal User user) {
        return ResponseEntity.ok(cartService.getCart(user));
    }

    // POST /api/cart — add item (user resolved from JWT, not from body)
    @PostMapping
    public ResponseEntity<CartResponse> addToCart(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody CartRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(cartService.addToCart(user, request));
    }

    // PUT /api/cart/{cartItemId} — update quantity
    @PutMapping("/{cartItemId}")
    public ResponseEntity<CartResponse> updateQuantity(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId,
            @RequestParam int quantity) {
        return ResponseEntity.ok(cartService.updateQuantity(user, cartItemId, quantity));
    }

    // DELETE /api/cart/{cartItemId} — remove single item (ownership checked in service)
    @DeleteMapping("/{cartItemId}")
    public ResponseEntity<Void> removeItem(
            @AuthenticationPrincipal User user,
            @PathVariable Long cartItemId) {
        cartService.removeItem(user, cartItemId);
        return ResponseEntity.noContent().build();
    }

    // DELETE /api/cart — clear entire cart
    @DeleteMapping
    public ResponseEntity<Void> clearCart(@AuthenticationPrincipal User user) {
        cartService.clearCart(user);
        return ResponseEntity.noContent().build();
    }
}
