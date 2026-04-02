package com.example.ecommerce_backend.service;

import com.example.ecommerce_backend.dto.request.CartRequest;
import com.example.ecommerce_backend.dto.response.CartResponse;
import com.example.ecommerce_backend.entity.Cart;
import com.example.ecommerce_backend.entity.Product;
import com.example.ecommerce_backend.entity.User;
import com.example.ecommerce_backend.exception.BadRequestException;
import com.example.ecommerce_backend.exception.ResourceNotFoundException;
import com.example.ecommerce_backend.exception.UnauthorizedException;
import com.example.ecommerce_backend.repository.CartRepository;
import com.example.ecommerce_backend.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;

    public List<CartResponse> getCart(User user) {
        return cartRepository.findByUser(user)
                .stream()
                .map(CartResponse::from)
                .toList();
    }

    @Transactional
    public CartResponse addToCart(User user, CartRequest request) {
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResourceNotFoundException("Product", request.getProductId()));

        if (product.getStock() < request.getQuantity()) {
            throw new BadRequestException("Only " + product.getStock() + " units in stock for: " + product.getName());
        }

        // If item already exists — update quantity instead of inserting a duplicate
        Cart cart = cartRepository.findByUserAndProduct(user, product)
                .map(existing -> {
                    int newQty = existing.getQuantity() + request.getQuantity();
                    if (product.getStock() < newQty) {
                        throw new BadRequestException("Not enough stock. Available: " + product.getStock());
                    }
                    existing.setQuantity(newQty);
                    return existing;
                })
                .orElseGet(() -> Cart.builder()
                        .user(user)
                        .product(product)
                        .quantity(request.getQuantity())
                        .build());

        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateQuantity(User user, Long cartItemId, int quantity) {
        Cart cart = getCartItemOwnedByUser(user, cartItemId);

        if (cart.getProduct().getStock() < quantity) {
            throw new BadRequestException("Only " + cart.getProduct().getStock() + " units in stock.");
        }

        cart.setQuantity(quantity);
        return CartResponse.from(cartRepository.save(cart));
    }

    @Transactional
    public void removeItem(User user, Long cartItemId) {
        Cart cart = getCartItemOwnedByUser(user, cartItemId);
        cartRepository.delete(cart);
    }

    @Transactional
    public void clearCart(User user) {
        cartRepository.deleteByUser(user);
    }

    // Ensures a user can only touch their own cart items
    private Cart getCartItemOwnedByUser(User user, Long cartItemId) {
        Cart cart = cartRepository.findById(cartItemId)
                .orElseThrow(() -> new ResourceNotFoundException("Cart item", cartItemId));
        if (!cart.getUser().getId().equals(user.getId())) {
            throw new UnauthorizedException("You do not own this cart item.");
        }
        return cart;
    }
}
