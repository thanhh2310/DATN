package com.example.demo.repository;

import com.example.demo.model.OrderReturn;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OrderReturnRepository extends JpaRepository<OrderReturn, Integer> {
    boolean existsByOrderId(Integer orderId);

    Optional<OrderReturn> findByOrderId(Integer orderId);

    Page<OrderReturn> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    Page<OrderReturn> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
