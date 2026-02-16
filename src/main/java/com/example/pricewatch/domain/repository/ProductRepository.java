package com.example.pricewatch.domain.repository;


import com.example.pricewatch.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
}