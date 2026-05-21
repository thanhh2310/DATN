package com.example.demo.scheduler;

import com.example.demo.model.Order;
import com.example.demo.repository.OrderRepository;
import com.example.demo.service.OrderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderService orderService; // Gọi thẳng Service

    @Scheduled(fixedRate = 60000)
    public void cancelExpiredUnpaidOrders() {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);

        List<Order> expiredOrders = orderRepository.findExpiredUnpaidOnlineOrders(
                Order.PaymentStatus.UNPAID,
                deadline
        );

        if (expiredOrders.isEmpty()) {
            return;
        }

        log.info("CronJob: Tìm thấy {} đơn hàng chưa thanh toán quá 15 phút. Tiến hành hủy...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // Gọi thẳng hàm cancelOrder đã thiết kế chuẩn trong OrderService
                // Tham số: orderId, currentUserId (truyền null vì hệ thống chạy), isAdmin = true
                orderService.cancelOrder(order.getId(), null, true);

                log.info("Đã hủy tự động đơn hàng ID: {}", order.getId());
            } catch (Exception e) {
                log.error("Lỗi khi hủy tự động đơn hàng ID {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}