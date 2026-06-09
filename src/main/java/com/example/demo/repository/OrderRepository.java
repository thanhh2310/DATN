package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.projection.DailyRevenueProjection;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public interface OrderRepository extends JpaRepository<Order, Integer> {
    List<Order> findByPaymentStatusAndCreatedAtBefore(Order.PaymentStatus status, LocalDateTime time);

    Page<Order> findByUserIdOrderByCreatedAtDesc(Integer userId, Pageable pageable);

    @Query("""
        SELECT o
        FROM Order o
        LEFT JOIN o.paymentMethod pm
        WHERE (:orderId IS NULL OR o.id = :orderId)
          AND (:paymentMethodCode IS NULL OR LOWER(pm.code) = LOWER(:paymentMethodCode))
          AND (:minTotal IS NULL OR o.totalAmount >= :minTotal)
          AND (:maxTotal IS NULL OR o.totalAmount <= :maxTotal)
    """)
    Page<Order> searchOrders(
            @Param("orderId") Integer orderId,
            @Param("paymentMethodCode") String paymentMethodCode,
            @Param("minTotal") BigDecimal minTotal,
            @Param("maxTotal") BigDecimal maxTotal,
            Pageable pageable
    );

    @Query("SELECT o FROM Order o WHERE o.paymentStatus = :status AND o.createdAt < :deadline AND o.paymentMethod.code != 'CASH'")
    List<Order> findExpiredUnpaidOnlineOrders(
            @Param("status") Order.PaymentStatus status,
            @Param("deadline") LocalDateTime deadline
    );

    // Lấy tổng doanh thu toàn hệ thống
    @Query("SELECT COALESCE(SUM(o.totalAmount), 0) FROM Order o WHERE o.paymentStatus = 'PAID' AND o.orderStatus NOT IN ('CANCELLED', 'RETURNED')")
    BigDecimal calculateTotalRevenue();

    // Đếm số đơn hàng theo trạng thái
    long countByOrderStatus(Order.OrderStatus status);

    // Đếm tổng số đơn thành công
    @Query("SELECT COUNT(o) FROM Order o WHERE o.orderStatus NOT IN ('CANCELLED', 'RETURNED')")
    long countTotalValidOrders();

    // Lấy doanh thu theo từng ngày (Dùng Native Query cho dễ xử lý Date)
    @Query(value = """
        SELECT 
            CAST(created_at AS DATE) AS reportDate,
            COUNT(id) AS totalOrders,
            COALESCE(SUM(total_amount), 0) AS totalRevenue
        FROM orders
        WHERE payment_status = 'PAID' 
          AND order_status NOT IN ('CANCELLED', 'RETURNED')
          AND created_at >= :startDate
        GROUP BY CAST(created_at AS DATE)
        ORDER BY reportDate ASC
    """, nativeQuery = true)
    List<DailyRevenueProjection> getDailyRevenue(@Param("startDate") LocalDate startDate);
}
