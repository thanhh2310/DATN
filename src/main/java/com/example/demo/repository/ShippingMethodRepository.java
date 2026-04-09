package com.example.demo.repository;

import com.example.demo.model.ShippingMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ShippingMethodRepository extends JpaRepository<ShippingMethod, Integer> {
    boolean existsByName(@NotBlank(message = "Name must not be blank") @Size(max = 100, message = "Name must be <= 100 characters") String name);
}
