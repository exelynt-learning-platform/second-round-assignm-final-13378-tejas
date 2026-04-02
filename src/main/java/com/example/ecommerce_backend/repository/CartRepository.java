package com.example.ecommerce_backend.repository;

import com.example.ecommerce_backend.entity.Cart;
import com.example.ecommerce_backend.entity.User;
import com.example.ecommerce_backend.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUser(User user);
    Optional<Cart> findByUserAndProduct(User user, Product product);
    void deleteByUser(User user);
}
