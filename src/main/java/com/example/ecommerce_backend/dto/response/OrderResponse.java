package com.example.ecommerce_backend.dto.response;

import com.example.ecommerce_backend.entity.Order;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderResponse {
    private Long orderId;
    private double totalPrice;
    private String orderStatus;
    private String paymentStatus;
    private String addressLine;
    private String city;
    private String state;
    private String postalCode;
    private String phone;
    private LocalDateTime createdAt;
    private List<ItemLine> items;

    @Data
    public static class ItemLine {
        private Long productId;
        private String productName;
        private int quantity;
        private double priceAtPurchase;
        private double subtotal;
    }

    public static OrderResponse from(Order order) {
        OrderResponse r = new OrderResponse();
        r.orderId       = order.getId();
        r.totalPrice    = order.getTotalPrice();
        r.orderStatus   = order.getStatus().name();
        r.paymentStatus = order.getPaymentStatus().name();
        r.addressLine   = order.getAddressLine();
        r.city           = order.getCity();
        r.state          = order.getState();
        r.postalCode     = order.getPostalCode();
        r.phone          = order.getPhone();
        r.createdAt      = order.getCreatedAt();
        r.items = order.getItems().stream().map(item -> {
            ItemLine il = new ItemLine();
            il.productId        = item.getProduct().getId();
            il.productName      = item.getProduct().getName();
            il.quantity         = item.getQuantity();
            il.priceAtPurchase  = item.getPriceAtPurchase();
            il.subtotal         = item.getSubtotal();
            return il;
        }).toList();
        return r;
    }
}
