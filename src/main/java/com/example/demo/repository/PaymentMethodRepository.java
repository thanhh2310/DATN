package com.example.demo.repository;

import com.example.demo.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.Optional;


public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    boolean existsByCode(String upperCase);

    Optional<PaymentMethod> findByCode(String code);

    List<PaymentMethod> findByIsActiveTrue();
}
