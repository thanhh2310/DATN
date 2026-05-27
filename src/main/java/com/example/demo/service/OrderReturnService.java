package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.OrderReturnDecisionRequest;
import com.example.demo.dto.request.OrderReturnRequest;
import com.example.demo.dto.response.OrderReturnResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderReturnService {
    private final OrderReturnRepository orderReturnRepository;
    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductSkuRepository skuRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository historyRepository;
    private final UserRepository userRepository;
    private final WalletService walletService;

    @Transactional
    public OrderReturnResponse createReturnRequest(Integer userId, Integer orderId, OrderReturnRequest request) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        if (!order.getUser().getId().equals(userId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (order.getOrderStatus() != Order.OrderStatus.DELIVERED) {
            throw new WebErrorConfig(ErrorCode.ORDER_NOT_DELIVERED);
        }

        if (order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new WebErrorConfig(ErrorCode.ORDER_NOT_PAID);
        }

        if (orderReturnRepository.existsByOrderId(orderId)) {
            throw new WebErrorConfig(ErrorCode.ORDER_RETURN_ALREADY_EXISTED);
        }

        OrderReturn orderReturn = OrderReturn.builder()
                .order(order)
                .user(order.getUser())
                .reason(request.getReason())
                .status(OrderReturn.ReturnStatus.PENDING)
                .build();
        orderReturn = orderReturnRepository.save(orderReturn);

        order.setOrderStatus(Order.OrderStatus.RETURN_REQUESTED);
        orderRepository.save(order);

        saveHistory(order, Order.OrderStatus.RETURN_REQUESTED.name(),
                "Khách hàng đã yêu cầu hoàn hàng. Lý do: " + request.getReason());

        return mapToResponse(orderReturn);
    }

    @Transactional
    public OrderReturnResponse approveReturn(Integer returnId, Integer managerId, OrderReturnDecisionRequest request) {
        OrderReturn orderReturn = getPendingReturn(returnId);
        Order order = orderReturn.getOrder();

        walletService.refundOrder(order, "Hoàn tiền hoàn hàng đơn #" + order.getId() + " vào ví");
        restoreStock(order);
        restoreCouponUsage(order);

        order.setOrderStatus(Order.OrderStatus.RETURNED);
        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrderId(order.getId()).orElse(null);
        if (payment != null) {
            payment.setStatus(Order.PaymentStatus.REFUNDED.name());
            paymentRepository.save(payment);
        }

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        orderReturn.setStatus(OrderReturn.ReturnStatus.APPROVED);
        orderReturn.setAdminNote(request != null ? request.getAdminNote() : null);
        orderReturn.setProcessedBy(manager);
        orderReturn.setProcessedAt(LocalDateTime.now());
        orderReturn = orderReturnRepository.save(orderReturn);

        saveHistory(order, Order.OrderStatus.RETURNED.name(),
                "Admin/Staff đã duyệt hoàn hàng. Đã hoàn tiền vào ví và nhập lại tồn kho.");

        return mapToResponse(orderReturn);
    }

    @Transactional
    public OrderReturnResponse rejectReturn(Integer returnId, Integer managerId, OrderReturnDecisionRequest request) {
        OrderReturn orderReturn = getPendingReturn(returnId);
        Order order = orderReturn.getOrder();

        order.setOrderStatus(Order.OrderStatus.RETURN_REJECTED);
        orderRepository.save(order);

        User manager = userRepository.findById(managerId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        orderReturn.setStatus(OrderReturn.ReturnStatus.REJECTED);
        orderReturn.setAdminNote(request != null ? request.getAdminNote() : null);
        orderReturn.setProcessedBy(manager);
        orderReturn.setProcessedAt(LocalDateTime.now());
        orderReturn = orderReturnRepository.save(orderReturn);

        saveHistory(order, Order.OrderStatus.RETURN_REJECTED.name(),
                "Admin/Staff đã từ chối yêu cầu hoàn hàng. Ghi chú: " + orderReturn.getAdminNote());

        return mapToResponse(orderReturn);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderReturnResponse> getMyReturns(Integer userId, int page, int size) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<OrderReturn> returnPage = orderReturnRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);
        return toPageResponse(returnPage, page);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderReturnResponse> getAllReturns(int page, int size) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);
        Page<OrderReturn> returnPage = orderReturnRepository.findAllByOrderByCreatedAtDesc(pageable);
        return toPageResponse(returnPage, page);
    }

    @Transactional(readOnly = true)
    public OrderReturnResponse getReturnById(Integer returnId) {
        return mapToResponse(orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_RETURN_NOT_FOUND)));
    }

    @Transactional(readOnly = true)
    public OrderReturnResponse getReturnByOrderId(Integer orderId, Integer currentUserId, boolean isManager) {
        OrderReturn orderReturn = orderReturnRepository.findByOrderId(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_RETURN_NOT_FOUND));

        if (!isManager && !orderReturn.getUser().getId().equals(currentUserId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        return mapToResponse(orderReturn);
    }

    private OrderReturn getPendingReturn(Integer returnId) {
        OrderReturn orderReturn = orderReturnRepository.findById(returnId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_RETURN_NOT_FOUND));

        if (orderReturn.getStatus() != OrderReturn.ReturnStatus.PENDING) {
            throw new WebErrorConfig(ErrorCode.ORDER_RETURN_ALREADY_PROCESSED);
        }

        return orderReturn;
    }

    private void restoreStock(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : orderItems) {
            skuRepository.incrementStock(item.getProductSku().getId(), item.getQuantity());
        }
    }

    private void restoreCouponUsage(Order order) {
        if (order.getCoupon() != null) {
            Coupon coupon = order.getCoupon();
            if (coupon.getUsedCount() > 0) {
                coupon.setUsedCount(coupon.getUsedCount() - 1);
            }
        }
    }

    private void saveHistory(Order order, String status, String notes) {
        historyRepository.save(OrderStatusHistory.builder()
                .order(order)
                .status(status)
                .notes(notes)
                .build());
    }

    private PageResponse<OrderReturnResponse> toPageResponse(Page<OrderReturn> returnPage, int requestedPage) {
        return PageResponse.<OrderReturnResponse>builder()
                .currentPage(requestedPage)
                .totalPage(returnPage.getTotalPages())
                .pageSize(returnPage.getSize())
                .totalElements(returnPage.getTotalElements())
                .items(returnPage.getContent().stream().map(this::mapToResponse).toList())
                .build();
    }

    private OrderReturnResponse mapToResponse(OrderReturn orderReturn) {
        User user = orderReturn.getUser();
        User processedBy = orderReturn.getProcessedBy();
        Order order = orderReturn.getOrder();

        return OrderReturnResponse.builder()
                .id(orderReturn.getId())
                .orderId(order.getId())
                .userId(user.getId())
                .userName(buildUserName(user))
                .userEmail(user.getEmail())
                .refundAmount(order.getTotalAmount())
                .reason(orderReturn.getReason())
                .adminNote(orderReturn.getAdminNote())
                .status(orderReturn.getStatus().name())
                .processedById(processedBy != null ? processedBy.getId() : null)
                .processedByName(processedBy != null ? buildUserName(processedBy) : null)
                .processedAt(orderReturn.getProcessedAt())
                .createdAt(orderReturn.getCreatedAt())
                .updatedAt(orderReturn.getUpdatedAt())
                .build();
    }

    private String buildUserName(User user) {
        String fullName = String.join(" ",
                user.getFirstName() != null ? user.getFirstName() : "",
                user.getLastName() != null ? user.getLastName() : ""
        ).trim();
        return fullName.isBlank() ? user.getEmail() : fullName;
    }
}
