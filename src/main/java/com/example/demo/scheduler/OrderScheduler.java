package com.example.demo.scheduler;

import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderScheduler {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository skuRepository;
    private final CouponRepository couponRepository; // 👉 Gọi thẳng Repository để update, ko qua Service để tránh vòng lặp logic
    private final OrderStatusHistoryRepository historyRepository;

    /**
     * Tác vụ chạy ngầm mỗi 1 phút (60,000 milliseconds)
     * Tìm và hủy các đơn hàng UNPAID đã tồn tại quá 15 phút
     */
    @Scheduled(fixedRate = 60000)
    @Transactional
    public void cancelExpiredUnpaidOrders() {
        // 1. Tính mốc thời gian: Hiện tại lùi về 15 phút trước
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);

        // 2. Tìm các đơn hàng thỏa mãn điều kiện
        List<Order> expiredOrders = orderRepository.findByPaymentStatusAndCreatedAtBefore(
                Order.PaymentStatus.UNPAID,
                deadline
        );

        if (expiredOrders.isEmpty()) {
            return; // Không có đơn nào thì thoát cho nhẹ Server
        }

        log.info("CronJob: Tìm thấy {} đơn hàng chưa thanh toán quá 15 phút. Đang tiến hành hủy...", expiredOrders.size());

        for (Order order : expiredOrders) {
            try {
                // 3. Cập nhật trạng thái đơn hàng
                order.setOrderStatus(Order.OrderStatus.CANCELLED);
                order.setPaymentStatus(Order.PaymentStatus.FAILED);
                orderRepository.save(order);

                // 4. Lấy danh sách sản phẩm trong đơn để hoàn lại Stock
                List<OrderItem> items = orderItemRepository.findByOrderId(order.getId()); // (Nhớ tạo hàm findByOrderId)
                for (OrderItem item : items) {
                    // Gọi hàm cộng kho trực tiếp dưới DB
                    skuRepository.incrementStock(item.getProductSku().getId(), item.getQuantity());
                }

                // 5. Hoàn lại lượt sử dụng Coupon (Nếu có)
                if (order.getCoupon() != null) {
                    Coupon coupon = order.getCoupon();
                    // Đảm bảo không bị trừ xuống số âm
                    if (coupon.getUsedCount() > 0) {
                        coupon.setUsedCount(coupon.getUsedCount() - 1);
                        couponRepository.save(coupon);
                    }
                }

                // 6. Ghi lại lịch sử
                OrderStatusHistory history = OrderStatusHistory.builder()
                        .order(order)
                        .status(Order.OrderStatus.CANCELLED.name())
                        .notes("Hệ thống tự động hủy do quá hạn thanh toán 15 phút")
                        .build();
                historyRepository.save(history);

                log.info("Đã hủy tự động đơn hàng ID: {}", order.getId());

            } catch (Exception e) {
                // Bắt try-catch trong vòng lặp để lỡ 1 đơn bị lỗi DB thì các đơn khác vẫn được hủy bình thường
                log.error("Lỗi khi hủy tự động đơn hàng ID {}: {}", order.getId(), e.getMessage());
            }
        }
    }
}