package com.example.demo.service;

import com.example.demo.model.Order;
import com.example.demo.projection.CategoryRevenueProjection;
import com.example.demo.projection.DailyRevenueProjection;
import com.example.demo.projection.TopProductProjection;
import com.example.demo.repository.OrderItemRepository;
import com.example.demo.repository.OrderRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public Map<String, Object> getOverviewStats() {
        Map<String, Object> stats = new HashMap<>();

        stats.put("totalUsers", userRepository.countByIsActiveTrue());
        stats.put("totalOrders", orderRepository.countTotalValidOrders());
        stats.put("totalRevenue", orderRepository.calculateTotalRevenue());
        stats.put("pendingOrders", orderRepository.countByOrderStatus(Order.OrderStatus.PENDING));

        return stats;
    }

    @Transactional(readOnly = true)
    public List<DailyRevenueProjection> getRevenueChart(int days) {
        LocalDate startDate = LocalDate.now().minusDays(days);
        return orderRepository.getDailyRevenue(startDate);
    }

    @Transactional(readOnly = true)
    public List<TopProductProjection> getTopProducts(int limit) {
        return orderItemRepository.getTopSellingProducts(limit);
    }

    @Transactional(readOnly = true)
    public List<CategoryRevenueProjection> getCategoryRevenue() {
        return orderItemRepository.getRevenueByCategory();
    }
}