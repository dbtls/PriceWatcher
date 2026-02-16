package com.example.pricewatch.domain.repository;


import com.example.pricewatch.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User,Long> {
    Optional<User> findByUserId(String userId);
    boolean existsByUserId(String userId);

}