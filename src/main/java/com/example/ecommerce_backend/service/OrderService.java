package com.example.ecommerce_backend.service;

import com.example.ecommerce_backend.dto.request.PlaceOrderRequest;
import com.example.ecommerce_backend.dto.response.OrderResponse;
import com.example.ecommerce_backend.entity.*;
import com.example.ecommerce_backend.exception.BadRequestException;
import com.example.ecommerce_backend.exception.ResourceNotFoundException;
import com.example.ecommerce_backend.exception.UnauthorizedException;
import com.example.ecommerce_backend.repository.CartRepository;
import com.example.ecommerce_backend.repository.OrderRepository;
import com.example.ecommerce_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;

    @Transactional
    public OrderResponse placeOrder(User user, PlaceOrderRequest request) {
        List<Cart> cartItems = cartRepository.findByUser(user);

        if (cartItems.isEmpty()) {
            throw new BadRequestException("Cannot place an order with an empty cart.");
        }

        // Build order items, validate stock, decrement stock
        List<OrderItem> orderItems = new ArrayList<>();
        double total = 0;

        for (Cart cartItem : cartItems) {
            Product product = cartItem.getProduct();

            if (product.getStock() < cartItem.getQuantity()) {
                throw new BadRequestException(
                        "Insufficient stock for '" + product.getName() +
                        "'. Available: " + product.getStock() +
                        ", Requested: " + cartItem.getQuantity());
            }

            // Decrement stock
            product.setStock(product.getStock() - cartItem.getQuantity());
            productRepository.save(product);

            // Snapshot the price at purchase time
            OrderItem item = OrderItem.builder()
                    .product(product)
                    .quantity(cartItem.getQuantity())
                    .priceAtPurchase(product.getPrice())   // real price from DB
                    .build();

            orderItems.add(item);
            total += item.getSubtotal();
        }

        // Build and save order
        Order order = Order.builder()
                .user(user)
                .totalPrice(total)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .addressLine(request.getAddressLine())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .phone(request.getPhone())
                .items(new ArrayList<>())
                .build();

        // Link items to order
        orderItems.forEach(item -> {
            item.setOrder(order);
            order.getItems().add(item);
        });

        Order saved = orderRepository.save(order);

        // Clear cart after successful order
        cartRepository.deleteByUser(user);

        log.info("Order #{} placed by user {}", saved.getId(), user.getEmail());
        return OrderResponse.from(saved);
    }

    public List<OrderResponse> getMyOrders(User user) {
        return orderRepository.findByUserOrderByCreatedAtDesc(user)
                .stream()
                .map(OrderResponse::from)
                .toList();
    }

    public OrderResponse getOrderById(User user, Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));

        // Users can only see their own orders; admins can see all
        if (!order.getUser().getId().equals(user.getId())
                && user.getRole() != User.Role.ADMIN) {
            throw new UnauthorizedException("You do not have access to this order.");
        }

        return OrderResponse.from(order);
    }

    // Called by StripeService after payment confirmation
    @Transactional
    public void markPaid(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setPaymentStatus(Order.PaymentStatus.PAID);
        order.setStatus(Order.OrderStatus.CONFIRMED);
        orderRepository.save(order);
        log.info("Order #{} marked as PAID", orderId);
    }

    @Transactional
    public void markFailed(Long orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order", orderId));
        order.setPaymentStatus(Order.PaymentStatus.FAILED);
        orderRepository.save(order);
        log.warn("Order #{} payment FAILED", orderId);
    }
}
