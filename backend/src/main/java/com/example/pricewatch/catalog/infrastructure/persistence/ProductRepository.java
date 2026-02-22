package com.example.pricewatch.catalog.infrastructure.persistence;


import com.example.pricewatch.catalog.domain.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProductRepository extends JpaRepository<Product,Long> {
}



