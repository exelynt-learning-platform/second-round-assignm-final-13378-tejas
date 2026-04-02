package com.example.ecommerce_backend.service;

import com.stripe.Stripe;
import com.stripe.exception.SignatureVerificationException;
import com.stripe.exception.StripeException;
import com.stripe.model.Event;
import com.stripe.model.PaymentIntent;
import com.stripe.net.Webhook;
import com.stripe.param.PaymentIntentCreateParams;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class StripeService {

    @Value("${stripe.secret.key}")
    private String secretKey;

    // Set Stripe key once at startup — not on every call
    @PostConstruct
    public void init() {
        Stripe.apiKey = secretKey;
        log.info("Stripe initialized");
    }

    /**
     * Creates a PaymentIntent and returns its clientSecret.
     * The frontend uses the clientSecret to confirm payment via Stripe.js.
     *
     * @param amountInPaise amount in smallest currency unit (₹1 = 100 paise)
     * @param orderId       stored as metadata for webhook reconciliation
     */
    public String createPaymentIntent(long amountInPaise, Long orderId) throws StripeException {
        if (amountInPaise < 50) {
            throw new IllegalArgumentException("Amount must be at least ₹0.50 (50 paise)");
        }

        PaymentIntentCreateParams params = PaymentIntentCreateParams.builder()
                .setAmount(amountInPaise)
                .setCurrency("inr")
                .putMetadata("orderId", String.valueOf(orderId))
                .build();

        PaymentIntent intent = PaymentIntent.create(params);
        log.info("Created PaymentIntent {} for order #{}", intent.getId(), orderId);
        return intent.getClientSecret();
    }

    /**
     * Verifies and parses an incoming Stripe webhook event.
     * Throws SignatureVerificationException if the signature doesn't match.
     */
    public Event constructWebhookEvent(String payload, String sigHeader, String webhookSecret)
            throws SignatureVerificationException {
        return Webhook.constructEvent(payload, sigHeader, webhookSecret);
    }
}
