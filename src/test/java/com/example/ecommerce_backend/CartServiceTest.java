package com.example.ecommerce_backend;

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
import com.example.ecommerce_backend.service.CartService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock CartRepository cartRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks CartService cartService;

    private User user;
    private User otherUser;
    private Product product;
    private CartRequest cartRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").role(User.Role.CUSTOMER).enabled(true).build();
        otherUser = User.builder().id(2L).email("other@example.com").role(User.Role.CUSTOMER).enabled(true).build();

        product = Product.builder()
                .id(10L)
                .name("Test Product")
                .price(500.0)
                .stock(20)
                .build();

        cartRequest = new CartRequest();
        cartRequest.setProductId(10L);
        cartRequest.setQuantity(2);
    }

    @Test
    void addToCart_newItem_savesAndReturnsResponse() {
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.empty());

        Cart savedCart = Cart.builder().id(1L).user(user).product(product).quantity(2).build();
        when(cartRepository.save(any(Cart.class))).thenReturn(savedCart);

        CartResponse response = cartService.addToCart(user, cartRequest);

        assertThat(response.getProductId()).isEqualTo(10L);
        assertThat(response.getQuantity()).isEqualTo(2);
        assertThat(response.getSubtotal()).isEqualTo(1000.0);
    }

    @Test
    void addToCart_existingItem_updatesQuantity() {
        Cart existing = Cart.builder().id(1L).user(user).product(product).quantity(3).build();
        when(productRepository.findById(10L)).thenReturn(Optional.of(product));
        when(cartRepository.findByUserAndProduct(user, product)).thenReturn(Optional.of(existing));
        when(cartRepository.save(any(Cart.class))).thenAnswer(inv -> inv.getArgument(0));

        cartRequest.setQuantity(2);
        CartResponse response = cartService.addToCart(user, cartRequest);

        // 3 existing + 2 requested = 5
        assertThat(response.getQuantity()).isEqualTo(5);
    }

    @Test
    void addToCart_insufficientStock_throwsBadRequest() {
        product.setStock(1);
        cartRequest.setQuantity(5);

        when(productRepository.findById(10L)).thenReturn(Optional.of(product));

        assertThatThrownBy(() -> cartService.addToCart(user, cartRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("stock");
    }

    @Test
    void addToCart_productNotFound_throwsNotFound() {
        when(productRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> cartService.addToCart(user, cartRequest))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void removeItem_ownedByUser_deletesSuccessfully() {
        Cart cart = Cart.builder().id(5L).user(user).product(product).quantity(1).build();
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));

        assertThatCode(() -> cartService.removeItem(user, 5L)).doesNotThrowAnyException();
        verify(cartRepository).delete(cart);
    }

    @Test
    void removeItem_notOwnedByUser_throwsUnauthorized() {
        Cart cart = Cart.builder().id(5L).user(otherUser).product(product).quantity(1).build();
        when(cartRepository.findById(5L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.removeItem(user, 5L))
                .isInstanceOf(UnauthorizedException.class);

        verify(cartRepository, never()).delete(any());
    }

    @Test
    void getCart_returnsOnlyUserItems() {
        Cart c1 = Cart.builder().id(1L).user(user).product(product).quantity(2).build();
        Cart c2 = Cart.builder().id(2L).user(user).product(product).quantity(1).build();

        when(cartRepository.findByUser(user)).thenReturn(List.of(c1, c2));

        List<CartResponse> result = cartService.getCart(user);

        assertThat(result).hasSize(2);
    }

    @Test
    void updateQuantity_validQuantity_updatesSuccessfully() {
        Cart cart = Cart.builder().id(1L).user(user).product(product).quantity(2).build();
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));
        when(cartRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        CartResponse response = cartService.updateQuantity(user, 1L, 5);

        assertThat(response.getQuantity()).isEqualTo(5);
    }

    @Test
    void updateQuantity_exceedsStock_throwsBadRequest() {
        product.setStock(3);
        Cart cart = Cart.builder().id(1L).user(user).product(product).quantity(1).build();
        when(cartRepository.findById(1L)).thenReturn(Optional.of(cart));

        assertThatThrownBy(() -> cartService.updateQuantity(user, 1L, 10))
                .isInstanceOf(BadRequestException.class);
    }
}
