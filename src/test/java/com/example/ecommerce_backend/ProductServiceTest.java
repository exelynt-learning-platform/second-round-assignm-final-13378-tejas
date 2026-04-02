package com.example.ecommerce_backend;

import com.example.ecommerce_backend.entity.Product;
import com.example.ecommerce_backend.exception.ResourceNotFoundException;
import com.example.ecommerce_backend.repository.ProductRepository;
import com.example.ecommerce_backend.service.ProductService;
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
class ProductServiceTest {

    @Mock ProductRepository productRepository;
    @InjectMocks ProductService productService;

    private Product product;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id(1L)
                .name("Test Phone")
                .description("A smartphone")
                .price(25000.0)
                .stock(10)
                .build();
    }

    @Test
    void create_savesAndReturnsProduct() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        Product result = productService.create(product);

        assertThat(result.getName()).isEqualTo("Test Phone");
        verify(productRepository).save(product);
    }

    @Test
    void getAll_returnsListOfProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<Product> result = productService.getAll();

        assertThat(result).hasSize(1);
    }

    @Test
    void getById_found_returnsProduct() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(product));

        Product result = productService.getById(1L);

        assertThat(result.getId()).isEqualTo(1L);
    }

    @Test
    void getById_notFound_throwsNotFound() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getById(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void update_existingProduct_updatesFields() {
        Product updated = Product.builder()
                .name("Updated Phone")
                .price(30000.0)
                .stock(5)
                .build();

        when(productRepository.findById(1L)).thenReturn(Optional.of(product));
        when(productRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        Product result = productService.update(1L, updated);

        assertThat(result.getName()).isEqualTo("Updated Phone");
        assertThat(result.getPrice()).isEqualTo(30000.0);
        assertThat(result.getStock()).isEqualTo(5);
    }

    @Test
    void delete_existingProduct_deletesSuccessfully() {
        when(productRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> productService.delete(1L)).doesNotThrowAnyException();
        verify(productRepository).deleteById(1L);
    }

    @Test
    void delete_nonExistingProduct_throwsNotFound() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> productService.delete(99L))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
