package com.example.ecommerce_backend.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Product name is required")
    @Column(nullable = false)
    private String name;

    private String description;

    @Positive(message = "Price must be greater than zero")
    @Column(nullable = false)
    private double price;

    @Min(value = 0, message = "Stock cannot be negative")
    @Column(nullable = false)
    private int stock;

    private String imageUrl;
}
