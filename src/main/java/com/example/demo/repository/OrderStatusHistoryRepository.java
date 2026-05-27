package com.example.demo.repository;

import com.example.demo.model.OrderStatusHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderStatusHistoryRepository extends JpaRepository<OrderStatusHistory, Integer> {
    Optional<OrderStatusHistory> findFirstByOrderIdAndStatusOrderByCreatedAtDesc(Integer orderId, String status);
}
