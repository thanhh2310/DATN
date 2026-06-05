package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.PlaceOrderRequest;
import com.example.demo.dto.response.OrderHistoryResponse;
import com.example.demo.dto.response.OrderResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.SkuAttributeResponse;
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

        if ("CASH".equalsIgnoreCase(paymentMethod.getCode())) {
            cartItemRepository.deleteAll(items);
        }

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
            orderRepository.save(order);

            payment.setStatus(Order.PaymentStatus.PAID.name());
            paymentRepository.save(payment);

            OrderStatusHistory paidHistory = OrderStatusHistory.builder()
                    .order(order)
                    .status(Order.OrderStatus.PENDING.name())
                    .notes("Khách hàng đã thanh toán đơn hàng bằng ví. Chờ admin xác nhận đơn hàng.")
                    .build();
            orderStatusHistoryRepository.save(paidHistory);

            deletePaidItemsFromCart(order);
            trackPurchaseForOrder(order);
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

            if (existingPayment != null) {
                existingPayment.setStatus(Order.PaymentStatus.PAID.name());
                existingPayment.setTransactionId(vnpTransactionNo);
                paymentRepository.save(existingPayment);
            }

            deletePaidItemsFromCart(order);
            trackPurchaseForOrder(order);

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
                .notes(isSuccess ? "Khách hàng đã thanh toán thành công qua VNPAY. Chờ admin xác nhận đơn hàng." : "Khách hàng hủy/thanh toán VNPAY thất bại. Hệ thống đã hủy đơn và hoàn lại tồn kho.")
                .build();
        orderStatusHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public Order getOrderById(Integer orderId) {
        return orderRepository.findById(orderId).orElse(null);
    }

    @Transactional(readOnly = true)
    public OrderHistoryResponse getOrderDetailById(Integer orderId, Integer currentUserId, boolean isManager) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        if (!isManager && (currentUserId == null || order.getUser() == null || !order.getUser().getId().equals(currentUserId))) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        List<OrderItem> orderItems = orderItemRepository.findByOrderId(orderId);
        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);

        return mapToOrderHistoryResponse(order, orderItems, payment);
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

        Map<Integer, Payment> paymentByOrderId = paymentRepository.findByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), payment -> payment, (first, second) -> first));

        List<OrderHistoryResponse> orderResponses = orderPage.getContent().stream()
                .map(order -> mapToOrderHistoryResponse(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of()),
                        paymentByOrderId.get(order.getId())
                ))
                .toList();

        return PageResponse.<OrderHistoryResponse>builder()
                .currentPage(page)
                .totalPage(orderPage.getTotalPages())
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .items(orderResponses)
                .build();
    }

    @Transactional
    public void deliverOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() != Order.OrderStatus.SHIPPED) {
            throw new WebErrorConfig(ErrorCode.INVALID_ORDER_STATUS);
        }

        // Đổi trạng thái giao hàng
        order.setOrderStatus(Order.OrderStatus.DELIVERED);

        String noteMessage = "Đơn hàng đã được giao thành công.";

        // Xử lý riêng cho đơn Tiền mặt (Thu tiền hộ)
        if ("CASH".equalsIgnoreCase(order.getPaymentMethod().getCode())) {
            order.setPaymentStatus(Order.PaymentStatus.PAID);
            noteMessage = "Giao hàng thành công. Đã thu tiền mặt (COD).";

            // Cập nhật cả bảng Payment nếu bạn có tracking
            Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
            if (payment != null) {
                payment.setStatus(Order.PaymentStatus.PAID.name());
                paymentRepository.save(payment);
            }
            trackPurchaseForOrder(order);
        }

        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.DELIVERED.name())
                .notes(noteMessage)
                .build();
        orderStatusHistoryRepository.save(history);
    }

    @Transactional
    public void cancelOrder(Integer orderId, Integer currentUserId, boolean isManager) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        // =========================================================================
        // 1. KIỂM TRA QUYỀN TRUY CẬP (SECURITY CHECK - CHỐNG HACK IDOR)
        // =========================================================================
        if (!isManager && ((currentUserId == null) || !order.getUser().getId().equals(currentUserId))) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        if (!isManager && order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Bạn chỉ có thể tự hủy đơn hàng khi đang chờ xử lý. Vui lòng liên hệ CSKH!");
        }

        // CHẶN BỔ SUNG: Không cho phép Admin/Staff hủy đơn khi hàng đã xuất kho (SHIPPED)
        if (isManager && (order.getOrderStatus() == Order.OrderStatus.SHIPPED || order.getOrderStatus() == Order.OrderStatus.DELIVERED)) {
            throw new RuntimeException("Không thể hủy đơn hàng đã xuất kho hoặc giao thành công. Vui lòng sử dụng luồng Trả Hàng (Return).");
        }

        if (order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Đơn hàng này đã bị hủy trước đó.");
        }

        // =========================================================================
        // 2. KIỂM TRA TRẠNG THÁI ĐƠN HÀNG HỢP LỆ ĐỂ HỦY
        // =========================================================================
        // Khách hàng (USER) chỉ được tự hủy khi đơn đang ở trạng thái PENDING
        if (!isManager && order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new RuntimeException("Bạn chỉ có thể tự hủy đơn hàng khi đang chờ xử lý. Vui lòng liên hệ CSKH!");
        }

        // Cả Admin và Hệ thống không được hủy đơn đã Giao thành công hoặc Đã hủy trước đó
        if (order.getOrderStatus() == Order.OrderStatus.DELIVERED || order.getOrderStatus() == Order.OrderStatus.CANCELLED) {
            throw new RuntimeException("Không thể hủy đơn hàng đã giao thành công hoặc đã bị hủy.");
        }

        // =========================================================================
        // 3. XỬ LÝ HOÀN TIỀN VÀ TRẠNG THÁI THANH TOÁN
        // =========================================================================
        String paymentCode = order.getPaymentMethod().getCode().toUpperCase();
        boolean isRefunded = false;

        if (("VNPAY".equals(paymentCode) || "WALLET".equals(paymentCode)) && order.getPaymentStatus() == Order.PaymentStatus.PAID) {
            walletService.refundOrder(order, "Hoàn tiền đơn hàng #" + order.getId() + " vào ví");
            order.setPaymentStatus(Order.PaymentStatus.REFUNDED);
            isRefunded = true;
        } else {
            order.setPaymentStatus(Order.PaymentStatus.FAILED);
        }

        // =========================================================================
        // 4. HOÀN LẠI TỒN KHO VÀ MÃ GIẢM GIÁ
        // =========================================================================
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

        // =========================================================================
        // 5. CẬP NHẬT DB (ORDER & PAYMENT)
        // =========================================================================
        order.setOrderStatus(Order.OrderStatus.CANCELLED);
        orderRepository.save(order);

        Payment payment = paymentRepository.findByOrderId(orderId).orElse(null);
        if (payment != null) {
            payment.setStatus(isRefunded ? Order.PaymentStatus.REFUNDED.name() : Order.PaymentStatus.FAILED.name());
            paymentRepository.save(payment);
        }

        // =========================================================================
        // 6. LƯU LỊCH SỬ GIAO DỊCH
        // =========================================================================
        String actionBy;
        if (!isManager) {
            actionBy = "Khách hàng";
        } else if (currentUserId == null) {
            actionBy = "Hệ thống tự động"; // Nhận biết từ Scheduler
        } else {
            actionBy = "Admin/Staff";
        }

        String noteMessage = isRefunded
                ? actionBy + " đã hủy đơn hàng. Hoàn tiền vào ví thành công."
                : actionBy + " đã hủy đơn hàng. Hoàn kho thành công.";

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.CANCELLED.name())
                .notes(noteMessage)
                .build();
        orderStatusHistoryRepository.save(history);
    }

// OrderService.java — Thêm method mới
    @Transactional
    public void shipOrder(Integer orderId) {
            Order order = orderRepository.findById(orderId)
                            .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

            if (order.getOrderStatus() != Order.OrderStatus.PROCESSING) {
                    throw new WebErrorConfig(ErrorCode.INVALID_ORDER_STATUS);
            }

            order.setOrderStatus(Order.OrderStatus.SHIPPED);
            orderRepository.save(order);
                    OrderStatusHistory history = OrderStatusHistory.builder()
                            .order(order)
                            .status(Order.OrderStatus.SHIPPED.name())
                            .notes("Đơn hàng đã được giao cho đơn vị vận chuyển")
                            .build();
            orderStatusHistoryRepository.save(history);
    }


    @Transactional
    public void confirmOrder(Integer orderId) {
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ORDER_NOT_FOUND));

        if (order.getOrderStatus() != Order.OrderStatus.PENDING) {
            throw new WebErrorConfig(ErrorCode.INVALID_ORDER_STATUS);
        }

        String paymentCode = order.getPaymentMethod().getCode();
        if (!"CASH".equalsIgnoreCase(paymentCode) && order.getPaymentStatus() != Order.PaymentStatus.PAID) {
            throw new WebErrorConfig(ErrorCode.ORDER_NOT_PAID);
        }

        order.setOrderStatus(Order.OrderStatus.PROCESSING);
        orderRepository.save(order);

        OrderStatusHistory history = OrderStatusHistory.builder()
                .order(order)
                .status(Order.OrderStatus.PROCESSING.name())
                .notes("Admin đã xác nhận đơn hàng")
                .build();
        orderStatusHistoryRepository.save(history);
    }

    // Gọi hàm này khi chắc chắn khách đã thanh toán thành công
    private void trackPurchaseForOrder(Order order) {
        List<OrderItem> orderItems = orderItemRepository.findByOrderId(order.getId());
        for (OrderItem item : orderItems) {
            userInteractionService.trackPurchase(
                    order.getUser().getId(),
                    item.getProductSku().getProduct().getId()
            );
        }
    }

    private void deletePaidItemsFromCart(Order order) {
        if (order.getUser() == null) {
            return;
        }

        cartRepository.findByUserId(order.getUser().getId()).ifPresent(cart -> {
            List<Integer> skuIds = orderItemRepository.findByOrderId(order.getId()).stream()
                    .map(item -> item.getProductSku().getId())
                    .distinct()
                    .toList();

            if (!skuIds.isEmpty()) {
                cartItemRepository.deletePaidItems(cart.getId(), skuIds);
            }
        });
    }

    @Transactional(readOnly = true)
    public PageResponse<OrderHistoryResponse> getAllOrdersPaginated(int page, int size) {
        int pageNumber = (page > 0) ? page - 1 : 0;
        Pageable pageable = PageRequest.of(pageNumber, size, org.springframework.data.domain.Sort.by("createdAt").descending());

        Page<Order> orderPage = orderRepository.findAll(pageable);

        List<Integer> orderIds = orderPage.getContent().stream()
                .map(Order::getId)
                .toList();

        Map<Integer, List<OrderItem>> itemsByOrderId = orderItemRepository
                .findByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.groupingBy(item -> item.getOrder().getId()));

        Map<Integer, Payment> paymentByOrderId = paymentRepository.findByOrderIdIn(orderIds)
                .stream()
                .collect(Collectors.toMap(payment -> payment.getOrder().getId(), payment -> payment, (first, second) -> first));

        List<OrderHistoryResponse> orderResponses = orderPage.getContent().stream()
                .map(order -> mapToOrderHistoryResponse(
                        order,
                        itemsByOrderId.getOrDefault(order.getId(), List.of()),
                        paymentByOrderId.get(order.getId())
                ))
                .toList();

        return PageResponse.<OrderHistoryResponse>builder()
                .currentPage(page)
                .totalPage(orderPage.getTotalPages())
                .pageSize(orderPage.getSize())
                .totalElements(orderPage.getTotalElements())
                .items(orderResponses)
                .build();
    }

    private OrderHistoryResponse mapToOrderHistoryResponse(Order order, List<OrderItem> orderItems, Payment payment) {
        User buyer = order.getUser();
        PaymentMethod paymentMethod = order.getPaymentMethod();
        ShippingMethod shippingMethod = order.getShippingMethod();
        Coupon coupon = order.getCoupon();

        List<OrderHistoryResponse.OrderItemPreviewResponse> itemPreviews = orderItems.stream()
                .map(this::mapToOrderItemPreviewResponse)
                .toList();

        return OrderHistoryResponse.builder()
                .orderId(order.getId())
                .buyerId(buyer != null ? buyer.getId() : null)
                .buyerName(buildBuyerName(buyer))
                .buyerEmail(buyer != null ? buyer.getEmail() : null)
                .buyerPhone(buyer != null ? buyer.getPhoneNumber() : null)
                .paymentMethodId(paymentMethod != null ? paymentMethod.getId() : null)
                .paymentMethodCode(paymentMethod != null ? paymentMethod.getCode() : null)
                .paymentMethodName(paymentMethod != null ? paymentMethod.getName() : null)
                .paymentProviderCode(payment != null ? payment.getProviderCode() : null)
                .paymentTransactionId(payment != null ? payment.getTransactionId() : null)
                .shippingAddress(order.getShippingAddress())
                .shippingCity(order.getShippingCity())
                .shippingMethodId(shippingMethod != null ? shippingMethod.getId() : null)
                .shippingMethodName(shippingMethod != null ? shippingMethod.getName() : null)
                .subtotal(order.getSubtotal())
                .shippingFee(order.getShippingFee())
                .discountAmount(order.getDiscountAmount())
                .couponId(coupon != null ? coupon.getId() : null)
                .couponCode(coupon != null ? coupon.getCode() : null)
                .couponDiscountType(coupon != null ? coupon.getDiscountType() : null)
                .couponDiscountValue(coupon != null ? coupon.getDiscountValue() : null)
                .totalAmount(order.getTotalAmount())
                .orderStatus(order.getOrderStatus().name())
                .paymentStatus(order.getPaymentStatus().name())
                .createdAt(order.getCreatedAt())
                .items(itemPreviews)
                .build();
    }

    private OrderHistoryResponse.OrderItemPreviewResponse mapToOrderItemPreviewResponse(OrderItem item) {
        ProductSku sku = item.getProductSku();
        Product product = sku.getProduct();

        return OrderHistoryResponse.OrderItemPreviewResponse.builder()
                .skuId(sku.getId())
                .orderItemId(item.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .skuCode(sku.getSkuCode())
                .imageUrl(helper.resolveSkuImage(sku))
                .quantity(item.getQuantity())
                .price(item.getPrice())
                .discount(item.getDiscount())
                .attributeValues(mapSkuAttributes(sku))
                .build();
    }

    private List<SkuAttributeResponse> mapSkuAttributes(ProductSku sku) {
        if (sku.getSkuValues() == null) {
            return List.of();
        }

        return sku.getSkuValues().stream()
                .map(skuValue -> {
                    AttributeValue attrVal = skuValue.getAttributeValue();
                    Attribute attr = attrVal.getAttribute();
                    return SkuAttributeResponse.builder()
                            .attributeId(attr.getId())
                            .attributeName(attr.getName())
                            .valueId(attrVal.getId())
                            .valueName(attrVal.getValue())
                            .description(attrVal.getDescription())
                            .build();
                })
                .toList();
    }

    private String buildBuyerName(User buyer) {
        if (buyer == null) {
            return null;
        }

        String fullName = String.join(" ",
                buyer.getFirstName() != null ? buyer.getFirstName() : "",
                buyer.getLastName() != null ? buyer.getLastName() : ""
        ).trim();

        return fullName.isBlank() ? buyer.getEmail() : fullName;
    }
}
