package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.PlaceOrderRequest;
import com.example.demo.dto.response.OrderHistoryResponse;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository skuRepository;

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final PaymentRepository paymentRepository;
    private final OrderStatusHistoryRepository orderStatusHistoryRepository;
    private final CouponRepository couponRepository;

    private final ShippingMethodRepository shippingMethodRepository;
    private final UserAddressRepository userAddressRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    private final CouponService couponService;
    private final PaymentService paymentService;

    @Transactional
    public OrderResponse placeOrder(Integer userId, PlaceOrderRequest request, HttpServletRequest req) {

        // 1. Cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CART_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        if (items.isEmpty()) {
            throw new WebErrorConfig(ErrorCode.CART_IS_EMPTY);
        }

        // 2. Validate stock
        validateStock(items);

        // 3. Subtotal
        BigDecimal subtotal = calculateSubtotal(items);

        // 4. Shipping
        ShippingMethod shippingMethod = shippingMethodRepository.findById(request.getShippingMethodId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SHIPPING_METHOD_NOT_FOUND));

        BigDecimal shippingFee = shippingMethod.getCost();

        // 5. Address
        UserAddress address = userAddressRepository.findById(request.getAddressId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(userId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // 6. Coupon
        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (request.getCouponCode() != null && !request.getCouponCode().isBlank()) {
            coupon = couponService.getValidCoupon(request.getCouponCode(), subtotal);

            if ("FREE_SHIPPING".equalsIgnoreCase(coupon.getDiscountType())) {
                discount = shippingFee;
            } else {
                discount = couponService.calculateDiscount(coupon, subtotal);
            }

            couponService.increaseUsage(coupon);
        }

        // 7. Payment method
        PaymentMethod paymentMethod = paymentMethodRepository.findById(request.getPaymentMethodId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PAYMENT_METHOD_NOT_FOUND));

        // 8. Total
        BigDecimal total = subtotal
                .add(shippingFee)
                .subtract(discount)
                .max(BigDecimal.ZERO);

        Order order = Order.builder()
                .user(cart.getUser())
                .coupon(coupon)
                .shippingMethod(shippingMethod)
                .paymentMethod(paymentMethod) // ĐÃ BỔ SUNG
                .subtotal(subtotal)         // Lưu lại để sau này FE hiển thị
                .shippingFee(shippingFee)   //
                .discountAmount(discount)
                .totalAmount(total)
                .orderStatus(Order.OrderStatus.PENDING) // ĐÃ FIX LỖI ENUM
                .paymentStatus(Order.PaymentStatus.UNPAID) // ĐÃ FIX LỖI ENUM
                .shippingAddress(address.getAddressLine1())
                .shippingCity(address.getCity())
                .build();

        order = orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.PENDING.name())
                .notes("Đơn hàng được tạo mới")
                .build();
        orderStatusHistoryRepository.save(history);

        List<OrderItem> orderItemsToSave = new ArrayList<>();

        for (CartItem item : items) {
            ProductSku sku = item.getProductSku();

            // Gọi hàm trừ kho thẳng dưới DB
            int updatedRows = skuRepository.decrementStock(sku.getId(), item.getQuantity());

            // Nếu updatedRows == 0 nghĩa là điều kiện (stock >= qty) bị sai -> Hết hàng
            if (updatedRows == 0) {
                throw new WebErrorConfig(ErrorCode.INSUFFICIENT_STOCK);
                // Transaction sẽ tự động Rollback toàn bộ đơn hàng!
            }

            OrderItem orderItem = OrderItem.builder()
                    .order(order)
                    .productSku(sku)
                    .quantity(item.getQuantity())
                    .price(sku.getPrice())
                    .discount(BigDecimal.ZERO)
                    .build();

            orderItemsToSave.add(orderItem);
        }

        orderItemRepository.saveAll(orderItemsToSave);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .providerCode(paymentMethod.getCode())
                .amount(total)
                .status(Order.PaymentStatus.UNPAID.name())
                .build();

        paymentRepository.save(payment);

        cartItemRepository.deleteByCartId(cart.getId());

        String paymentUrl = null;

        // Nếu khách chọn VNPAY (Dựa vào code trong bảng payment_methods)
        if ("VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
            // Lời nhắn hiển thị trên trang VNPAY
            String orderInfo = "Thanh toan don hang " + order.getId();

            // Gọi PaymentService để tạo URL. (Ép kiểu BigDecimal về int vì VNPAY yêu cầu số nguyên)
            paymentUrl = paymentService.createVnPayPayment(order.getId(), total.intValue(), orderInfo, req);
        }

        return OrderResponse.builder()
                .orderId(order.getId())
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .paymentUrl(paymentUrl)
                .build();
    }

    private void validateStock(List<CartItem> items) {
        for (CartItem item : items) {
            ProductSku sku = item.getProductSku();

            if (item.getQuantity() > sku.getStockQuantity()) {
                throw new WebErrorConfig(ErrorCode.INSUFFICIENT_STOCK);
            }
        }
    }

    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProductSku().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    @Transactional
    public void handlePaymentResult(Integer orderId, boolean isSuccess, String vnpTransactionNo) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new RuntimeException("Đơn hàng không tồn tại"));

        // 1. Tránh duplicate callback hoặc xử lý lại đơn đã hủy
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID ||
                order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            return;
        }

        if (isSuccess) {
            // ================= THANH TOÁN THÀNH CÔNG =================
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setOrderStatus(Order.OrderStatus.PROCESSING);

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(Order.PaymentStatus.PAID.name());
                payment.setTransactionId(vnpTransactionNo);
                paymentRepository.save(payment);
            }

        } else {
            // ================= THANH TOÁN THẤT BẠI (ROLLBACK) =================
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setOrderStatus(Order.OrderStatus.CANCELLED);

            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(Order.PaymentStatus.FAILED.name());
                paymentRepository.save(payment);
            }

            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : orderItems) {
                skuRepository.incrementStock(item.getProductSku().getId(), item.getQuantity());
            }

            if (order.getCoupon() != null) {
                Coupon coupon = order.getCoupon();
                if (coupon.getUsedCount() > 0) {
                    coupon.setUsedCount(coupon.getUsedCount() - 1);
                    couponRepository.save(coupon);
                }
            }
        }

        orderRepository.save(order);

        // 2. Lưu lịch sử đơn hàng
        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(order.getOrderStatus().name())
                .notes(isSuccess ? "Khách hàng đã thanh toán thành công qua VNPAY" : "Khách hàng hủy/thanh toán VNPAY thất bại. Hệ thống đã hủy đơn và hoàn lại tồn kho.")
                .build();
        orderStatusHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderHistoryResponse> getMyOrderHistory(Integer userId, int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size);

        Page<Order> orderPage = orderRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable);

        // Lấy tất cả orderIds trong trang này
        List<Integer> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        // 1 query duy nhất lấy tất cả items
        Map<Integer, List<OrderItem>> itemsByOrderId = orderItemRepository
                .findByOrderIdIn(orderIds)  // Thêm method này vào Repository
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        List<OrderHistoryResponse> orderResponses = orderPage.getContent().stream().map(order -> {
            List<OrderHistoryResponse.OrderItemPreviewResponse> itemPreviews = itemsByOrderId
                    .getOrDefault(order.getId(), List.of())
                    .stream()
                    .map(item -> OrderHistoryResponse.OrderItemPreviewResponse.builder()
                            .skuId(item.getProductSku().getId())
                            .productName(item.getProductSku().getProduct().getName())
                            .imageUrl(item.getProductSku().getImageUrl() != null ?
                                    item.getProductSku().getImageUrl() :
                                    item.getProductSku().getProduct().getImages().stream()
                                            .findFirst().map(ProductImage::getImageUrl).orElse(null))
                            .quantity(item.getQuantity())
                            .price(item.getPrice())
                            .build())
                    .toList();

            return OrderHistoryResponse.builder()
                    .orderId(order.getId())
                    .totalAmount(order.getTotalAmount())
                    .orderStatus(order.getOrderStatus().name())
                    .paymentStatus(order.getPaymentStatus().name())
                    .createdAt(order.getCreatedAt())
                    .items(itemPreviews)
                    .build();
        }).toList();

        return PageResponse.<OrderHistoryResponse>builder()
                .currentPage(page)
                .totalPage(orderPage.getTotalPages())
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .data(orderResponses)
                .build();
    }
}