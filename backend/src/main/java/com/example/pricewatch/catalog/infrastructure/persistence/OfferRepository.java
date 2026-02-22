package com.example.pricewatch.catalog.infrastructure.persistence;

import com.example.pricewatch.catalog.domain.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer,Long> {
}




