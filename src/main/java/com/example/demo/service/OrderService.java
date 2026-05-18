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
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final UserInteractionRepository userInteractionRepository;

    private final ShippingMethodRepository shippingMethodRepository;
    private final UserAddressRepository userAddressRepository;
    private final PaymentMethodRepository paymentMethodRepository;

    private final CouponService couponService;
    private final PaymentService paymentService;
    private final WalletService walletService;
    private final UserInteractionService userInteractionService;
    private final Helper helper;

    @Transactional
    public OrderResponse placeOrder(Integer userId, PlaceOrderRequest request, HttpServletRequest req) {

        // 1. Cart
        Cart cart = cartRepository.findByUserId(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CART_NOT_FOUND));

        List<CartItem> items = cartItemRepository.findSelectedItems(cart.getId(), request.getCartItemIds());

        if (items.isEmpty()) {
            throw new WebErrorConfig(ErrorCode.SELECTED_ITEMS_NOT_FOUND);
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
        List<UserInteraction> interactionsToSave = new ArrayList<>();

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

            UserInteraction interaction = UserInteraction.builder()
                    .user(userRepository.findById(userId).orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND)))
                    .sessionId(cart.getSessionId()) // Có thể null nhưng vẫn lưu
                    .product(productRepository.findById(sku.getProduct().getId()).orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND)))
                    .interactionType(UserInteraction.InteractionType.PURCHASE)
                    .interactionWeight(5.0f) // Mua hàng được 5 điểm trọng số
                    .build();
            interactionsToSave.add(interaction);
        }

        orderItemRepository.saveAll(orderItemsToSave);
        userInteractionRepository.saveAll(interactionsToSave);

        Payment payment = Payment.builder()
                .order(order)
                .paymentMethod(paymentMethod)
                .providerCode(paymentMethod.getCode())
                .amount(total)
                .status(Order.PaymentStatus.UNPAID.name())
                .build();

        paymentRepository.save(payment);

//        cartItemRepository.deleteByCartId(cart.getId());
        cartItemRepository.deleteAll(items);

        String paymentUrl = null;

        // Nếu khách chọn VNPAY (Dựa vào code trong bảng payment_methods)
        if ("VNPAY".equalsIgnoreCase(paymentMethod.getCode())) {
            // Lời nhắn hiển thị trên trang VNPAY
            String orderInfo = "Thanh toan don hang " + order.getId();

            // Gọi PaymentService để tạo URL. (Ép kiểu BigDecimal về int vì VNPAY yêu cầu số nguyên)
            paymentUrl = paymentService.createVnPayPayment(order.getId(), total.intValue(), orderInfo, req);
        } else if ("WALLET".equalsIgnoreCase(paymentMethod.getCode())) {
            walletService.payOrder(userId, order);

            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setOrderStatus(Order.OrderStatus.PROCESSING);
            orderRepository.save(order);

            payment.setStatus(Order.PaymentStatus.PAID.name());
            paymentRepository.save(payment);

            OrderStatusHistory paidHistory = OrderStatusHistory.builder()
                    .order(order)
                    .status(Order.OrderStatus.PROCESSING.name())
                    .notes("Khách hàng đã thanh toán đơn hàng bằng ví")
                    .build();
            orderStatusHistoryRepository.save(paidHistory);
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

        // Idempotent: Check payment record trước để tránh xử lý trùng khi VNPAY gọi callback nhiều lần
        Payment existingPayment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (existingPayment != null && vnpTransactionNo != null
                && existingPayment.getTransactionId() != null
                && existingPayment.getTransactionId().equals(vnpTransactionNo)) {
            return;
        }

        // Tránh duplicate callback hoặc xử lý lại đơn đã hủy
        if (order.getPaymentStatus() == Order.PaymentStatus.PAID ||
                order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            return;
        }

        if (isSuccess) {
            // ================= THANH TOÁN THÀNH CÔNG =================
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            order.setOrderStatus(Order.OrderStatus.PROCESSING);

            if (existingPayment != null) {
                existingPayment.setStatus(Order.PaymentStatus.PAID.name());
                existingPayment.setTransactionId(vnpTransactionNo);
                paymentRepository.save(existingPayment);
            }

            // Track PURCHASE interaction cho từng sản phẩm trong đơn
            List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
            for (OrderItem item : orderItems) {
                userInteractionService.trackPurchase(
                        order.getUser().getId(),
                        item.getProductSku().getProduct().getId()
                );
            }

        } else {
            // ================= THANH TOÁN THẤT BẠI (ROLLBACK) =================
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
            order.setOrderStatus(Order.OrderStatus.CANCELLED);

            if (existingPayment != null) {
                existingPayment.setStatus(Order.PaymentStatus.FAILED.name());
                paymentRepository.save(existingPayment);
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

        // Lưu lịch sử đơn hàng
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
                            .orderItemId(item.getId())
                            .productId(item.getProductSku().getProduct().getId())
                            .productName(item.getProductSku().getProduct().getName())
                            .imageUrl(helper.resolveSkuImage(item.getProductSku()))
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
                .items(orderResponses)
                .build();
    }

    public void completeCodOrder(Integer orderId){
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        // Kiểm tra xem đúng là đơn CASH không
        if (!"CASH".equalsIgnoreCase(order.getPaymentMethod().getCode())) {
            throw new RuntimeException("Chỉ áp dụng cho đơn thanh toán tiền mặt!");
        }

        // Đổi trạng thái khi giao hàng thành công
        order.setOrderStatus(Order.OrderStatus.DELIVERED); // Giao xong
        order.setPaymentStatus(Order.PaymentStatus.PAID);  // Đã nhận được tiền
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.PROCESSING.name())
                .notes("Khách hàng đã thanh toán bằng Tiền mặt (COD) thành công")
                .build();
        orderStatusHistoryRepository.save(history);
    }

    @Transactional
    public void refundOrderToWallet(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));
        String paymentCode = order.getPaymentMethod().getCode().toUpperCase();

        if ("VNPAY".equals(paymentCode) || "WALLET".equals(paymentCode)) {
            if (order.getPaymentStatus() == Order.PaymentStatus.PAID) {
                walletService.refundOrder(order, "Hoàn tiền đơn hàng #" + order.getId() + " vào ví");
                order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            }
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

        order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            payment.setStatus(Order.PaymentStatus.REFUNDED.name());
            paymentRepository.save(payment);
        }

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.CANCELLED.name())
                .notes("Đơn hàng đã được hoàn tiền vào ví")
                .build();
        orderStatusHistoryRepository.save(history);
    }


}
