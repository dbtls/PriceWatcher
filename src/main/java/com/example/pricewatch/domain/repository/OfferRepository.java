package com.example.pricewatch.domain.repository;

import com.example.pricewatch.domain.model.Offer;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OfferRepository extends JpaRepository<Offer,Long> {
}
