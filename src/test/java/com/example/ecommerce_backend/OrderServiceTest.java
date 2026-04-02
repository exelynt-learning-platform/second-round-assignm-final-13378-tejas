package com.example.ecommerce_backend;

import com.example.ecommerce_backend.dto.request.PlaceOrderRequest;
import com.example.ecommerce_backend.dto.response.OrderResponse;
import com.example.ecommerce_backend.entity.*;
import com.example.ecommerce_backend.exception.BadRequestException;
import com.example.ecommerce_backend.exception.UnauthorizedException;
import com.example.ecommerce_backend.repository.CartRepository;
import com.example.ecommerce_backend.repository.OrderRepository;
import com.example.ecommerce_backend.repository.ProductRepository;
import com.example.ecommerce_backend.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock CartRepository cartRepository;
    @Mock OrderRepository orderRepository;
    @Mock ProductRepository productRepository;

    @InjectMocks OrderService orderService;

    private User user;
    private Product product;
    private PlaceOrderRequest orderRequest;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("user@example.com").role(User.Role.CUSTOMER).enabled(true).build();

        product = Product.builder()
                .id(10L)
                .name("Laptop")
                .price(50000.0)
                .stock(5)
                .build();

        orderRequest = new PlaceOrderRequest();
        orderRequest.setAddressLine("123 Main St");
        orderRequest.setCity("Pune");
        orderRequest.setState("Maharashtra");
        orderRequest.setPostalCode("411001");
        orderRequest.setPhone("9876543210");
    }

    @Test
    void placeOrder_validCart_createsOrderAndDecrementsStock() {
        Cart cartItem = Cart.builder().id(1L).user(user).product(product).quantity(2).build();
        when(cartRepository.findByUser(user)).thenReturn(List.of(cartItem));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        Order savedOrder = Order.builder()
                .id(100L)
                .user(user)
                .totalPrice(100000.0)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .addressLine(orderRequest.getAddressLine())
                .city(orderRequest.getCity())
                .state(orderRequest.getState())
                .postalCode(orderRequest.getPostalCode())
                .phone(orderRequest.getPhone())
                .items(new ArrayList<>())
                .build();

        when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);

        OrderResponse response = orderService.placeOrder(user, orderRequest);

        assertThat(response.getOrderId()).isEqualTo(100L);
        // Stock should have been decremented
        assertThat(product.getStock()).isEqualTo(3);
        verify(cartRepository).deleteByUser(user);
    }

    @Test
    void placeOrder_emptyCart_throwsBadRequest() {
        when(cartRepository.findByUser(user)).thenReturn(List.of());

        assertThatThrownBy(() -> orderService.placeOrder(user, orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("empty cart");
    }

    @Test
    void placeOrder_insufficientStock_throwsBadRequest() {
        product.setStock(1);
        Cart cartItem = Cart.builder().id(1L).user(user).product(product).quantity(3).build();
        when(cartRepository.findByUser(user)).thenReturn(List.of(cartItem));

        assertThatThrownBy(() -> orderService.placeOrder(user, orderRequest))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Insufficient stock");

        // Stock must NOT be decremented when order fails
        assertThat(product.getStock()).isEqualTo(1);
        verify(orderRepository, never()).save(any());
    }

    @Test
    void getOrderById_ownOrder_returnsOrder() {
        Order order = Order.builder()
                .id(1L).user(user)
                .totalPrice(1000.0)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        OrderResponse response = orderService.getOrderById(user, 1L);

        assertThat(response.getOrderId()).isEqualTo(1L);
    }

    @Test
    void getOrderById_otherUsersOrder_throwsUnauthorized() {
        User otherUser = User.builder().id(99L).email("other@example.com").role(User.Role.CUSTOMER).enabled(true).build();
        Order order = Order.builder()
                .id(1L).user(otherUser)
                .totalPrice(1000.0)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.getOrderById(user, 1L))
                .isInstanceOf(UnauthorizedException.class);
    }

    @Test
    void getOrderById_adminCanViewAnyOrder() {
        User admin = User.builder().id(2L).email("admin@example.com").role(User.Role.ADMIN).enabled(true).build();
        Order order = Order.builder()
                .id(1L).user(user)
                .totalPrice(1000.0)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));

        assertThatCode(() -> orderService.getOrderById(admin, 1L)).doesNotThrowAnyException();
    }

    @Test
    void markPaid_updatesStatusCorrectly() {
        Order order = Order.builder()
                .id(1L).user(user)
                .totalPrice(1000.0)
                .status(Order.OrderStatus.PLACED)
                .paymentStatus(Order.PaymentStatus.PENDING)
                .items(new ArrayList<>())
                .build();

        when(orderRepository.findById(1L)).thenReturn(Optional.of(order));
        when(orderRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        orderService.markPaid(1L);

        assertThat(order.getPaymentStatus()).isEqualTo(Order.PaymentStatus.PAID);
        assertThat(order.getStatus()).isEqualTo(Order.OrderStatus.CONFIRMED);
    }

    @Test
    void placeOrder_totalPriceCalculatedCorrectly() {
        Product p1 = Product.builder().id(1L).name("Phone").price(20000.0).stock(10).build();
        Product p2 = Product.builder().id(2L).name("Case").price(500.0).stock(10).build();

        Cart c1 = Cart.builder().id(1L).user(user).product(p1).quantity(1).build();
        Cart c2 = Cart.builder().id(2L).user(user).product(p2).quantity(3).build();

        when(cartRepository.findByUser(user)).thenReturn(List.of(c1, c2));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Expected: (20000*1) + (500*3) = 21500
        when(orderRepository.save(any(Order.class))).thenAnswer(inv -> {
            Order o = inv.getArgument(0);
            assertThat(o.getTotalPrice()).isEqualTo(21500.0);
            o.setId(1L);
            return o;
        });

        orderService.placeOrder(user, orderRequest);
    }
}
