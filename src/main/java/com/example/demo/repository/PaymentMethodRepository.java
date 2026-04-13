package com.example.demo.repository;

import com.example.demo.model.PaymentMethod;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;


public interface PaymentMethodRepository extends JpaRepository<PaymentMethod, Integer> {
    boolean existsByCode(String upperCase);

    List<PaymentMethod> findByIsActiveTrue();
}
