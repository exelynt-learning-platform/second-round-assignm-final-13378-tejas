package com.example.ecommerce_backend.controller;

import com.example.ecommerce_backend.dto.response.OrderResponse;
import com.example.ecommerce_backend.entity.User;
import com.example.ecommerce_backend.exception.BadRequestException;
import com.example.ecommerce_backend.repository.OrderRepository;
import com.example.ecommerce_backend.service.OrderService;
import com.example.ecommerce_backend.service.StripeService;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.model.StripeObject;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.Optional;

@Slf4j
@RestController
@RequestMapping("/api/payment")
@RequiredArgsConstructor
public class PaymentController {

    private final StripeService stripeService;
    private final OrderService orderService;
    private final OrderRepository orderRepository;

    @Value("${stripe.webhook.secret:}")
    private String webhookSecret;

    /**
     * POST /api/payment/initiate/{orderId}
     * Creates a Stripe PaymentIntent and returns the clientSecret to the frontend.
     * The frontend uses it to confirm the payment via Stripe.js — never server-side.
     */
    @PostMapping("/initiate/{orderId}")
    public ResponseEntity<Map<String, String>> initiatePayment(
            @AuthenticationPrincipal User user,
            @PathVariable Long orderId) throws Exception {

        OrderResponse order = orderService.getOrderById(user, orderId);

        // Convert rupees → paise (Stripe uses smallest currency unit)
        long amountInPaise = Math.round(order.getTotalPrice() * 100);

        String clientSecret = stripeService.createPaymentIntent(amountInPaise, orderId);

        return ResponseEntity.ok(Map.of("clientSecret", clientSecret));
    }

    /**
     * POST /api/payment/webhook
     * Stripe calls this endpoint after payment events.
     * Verifies the Stripe-Signature header before processing.
     * Configure this URL in your Stripe dashboard.
     */
    @PostMapping("/webhook")
    public ResponseEntity<String> handleWebhook(
            @RequestBody String payload,
            @RequestHeader("Stripe-Signature") String sigHeader) {

        if (webhookSecret.isBlank()) {
            log.error("Stripe webhook secret not configured");
            return ResponseEntity.internalServerError().body("Webhook secret not set");
        }

        Event event;
        try {
            event = stripeService.constructWebhookEvent(payload, sigHeader, webhookSecret);
        } catch (SignatureVerificationException e) {
            log.warn("Invalid Stripe webhook signature");
            return ResponseEntity.badRequest().body("Invalid signature");
        }

        Optional<StripeObject> stripeObject = event.getDataObjectDeserializer().getObject();

        switch (event.getType()) {
            case "payment_intent.succeeded" -> {
                stripeObject.ifPresent(obj -> {
                    PaymentIntent intent = (PaymentIntent) obj;
                    Long orderId = Long.parseLong(intent.getMetadata().get("orderId"));
                    orderService.markPaid(orderId);
                    log.info("Payment succeeded for order #{}", orderId);
                });
            }
            case "payment_intent.payment_failed" -> {
                stripeObject.ifPresent(obj -> {
                    PaymentIntent intent = (PaymentIntent) obj;
                    Long orderId = Long.parseLong(intent.getMetadata().get("orderId"));
                    orderService.markFailed(orderId);
                    log.warn("Payment failed for order #{}", orderId);
                });
            }
            default -> log.debug("Unhandled Stripe event type: {}", event.getType());
        }

        return ResponseEntity.ok("Received");
    }
}
