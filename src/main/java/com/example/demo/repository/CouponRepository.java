package com.example.demo.repository;

import com.example.demo.model.Coupon;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface CouponRepository extends JpaRepository<Coupon, Integer> {
    Optional<Coupon> findByCode(String code);

    boolean existsByCode(@NotBlank(message = "Code must not be blank") @Size(max = 50, message = "Code must be <= 50 characters") String code);
}
