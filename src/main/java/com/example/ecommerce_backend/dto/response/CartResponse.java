package com.example.ecommerce_backend.dto.response;

import com.example.ecommerce_backend.entity.Cart;
import lombok.Data;

@Data
public class CartResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private double productPrice;
    private int quantity;
    private double subtotal;

    public static CartResponse from(Cart cart) {
        CartResponse r = new CartResponse();
        r.cartItemId    = cart.getId();
        r.productId     = cart.getProduct().getId();
        r.productName   = cart.getProduct().getName();
        r.productPrice  = cart.getProduct().getPrice();
        r.quantity       = cart.getQuantity();
        r.subtotal       = cart.getProduct().getPrice() * cart.getQuantity();
        return r;
    }
}
