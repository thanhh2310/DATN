package com.example.demo.repository;

import com.example.demo.model.OrderItem;
import com.example.demo.projection.CategoryRevenueProjection;
import com.example.demo.projection.TopProductProjection;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface OrderItemRepository extends JpaRepository<OrderItem, Integer> {
    List<OrderItem> findByOrderId(Integer id);

    List<OrderItem> findByOrderIdIn(List<Integer> orderIds);

    @Query(value = """
        SELECT 
            p.id AS productId,
            p.name AS productName,
            CAST(SUM(oi.quantity) AS INTEGER) AS totalSold,
            COALESCE(SUM(oi.quantity * oi.price), 0) AS totalRevenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN product_skus ps ON oi.product_sku_id = ps.id
        JOIN products p ON ps.product_id = p.id
        WHERE o.payment_status = 'PAID' AND o.order_status NOT IN ('CANCELLED', 'RETURNED')
        GROUP BY p.id, p.name
        ORDER BY totalSold DESC
        LIMIT :limit
    """, nativeQuery = true)
    List<TopProductProjection> getTopSellingProducts(@Param("limit") int limit);

    @Query(value = """
        SELECT 
            c.name AS categoryName,
            COALESCE(SUM(oi.quantity * oi.price), 0) AS revenue
        FROM order_items oi
        JOIN orders o ON oi.order_id = o.id
        JOIN product_skus ps ON oi.product_sku_id = ps.id
        JOIN products p ON ps.product_id = p.id
        JOIN categories c ON p.category_id = c.id
        WHERE o.payment_status = 'PAID' AND o.order_status NOT IN ('CANCELLED', 'RETURNED')
        GROUP BY c.name
        ORDER BY revenue DESC
    """, nativeQuery = true)
    List<CategoryRevenueProjection> getRevenueByCategory();

    @Query("""
        SELECT COALESCE(SUM(oi.quantity), 0)
        FROM OrderItem oi
        WHERE oi.productSku.product.id = :productId
          AND oi.order.paymentStatus = com.example.demo.model.Order.PaymentStatus.PAID
          AND oi.order.orderStatus NOT IN (
              com.example.demo.model.Order.OrderStatus.CANCELLED,
              com.example.demo.model.Order.OrderStatus.RETURNED
          )
    """)
    Long countSoldByProductId(@Param("productId") Integer productId);
}
