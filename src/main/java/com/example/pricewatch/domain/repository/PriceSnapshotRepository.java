package com.example.pricewatch.domain.repository;

import com.example.pricewatch.domain.model.PriceSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface PriceSnapshotRepository extends JpaRepository<PriceSnapshot,Long> {

}
