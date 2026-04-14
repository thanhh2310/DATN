package com.example.demo.repository;

import com.example.demo.model.Order;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByPaymentStatusAndCreatedAtBefore(Order.PaymentStatus status, LocalDateTime time);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);
}
