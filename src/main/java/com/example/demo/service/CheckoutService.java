package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.CheckoutPreviewRequest;
import com.example.demo.dto.response.*;
import com.example.demo.mapper.CouponMapper;
import com.example.demo.mapper.ShippingMethodMapper;
import com.example.demo.mapper.UserAddressMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CheckoutService {

    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;

    private final CouponService couponService;
    private final CouponMapper couponMapper;

    private final ShippingMethodRepository shippingMethodRepository;
    private final ShippingMethodMapper shippingMethodMapper;

    private final UserAddressRepository userAddressRepository;
    private final UserAddressMapper userAddressMapper;

    private final PaymentMethodRepository paymentMethodRepository;
    private final Helper helper;

    @Transactional(readOnly = true)
    public CheckoutPreviewResponse preview(Integer userId, CheckoutPreviewRequest request) {

        // 1. Cart
        Cart cart = getCart(userId);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        if (items.isEmpty()) {
            throw new WebErrorConfig(ErrorCode.CART_IS_EMPTY);
        }

        // 2. Map items
        List<CartItemResponse> itemResponses = items.stream()
                .map(this::mapToCartItemResponse)
                .toList();

        // 3. Subtotal
        BigDecimal subtotal = calculateSubtotal(items);

        // 4. Shipping
        ShippingMethod shippingMethod = getShippingMethod(request.getShippingMethodId());
        BigDecimal shippingFee = shippingMethod.getCost();

        // 5. Address
        UserAddress address = getAddress(userId, request.getAddressId());

        // 6. Coupon
        Coupon coupon = null;
        BigDecimal discount = BigDecimal.ZERO;

        if (hasCoupon(request)) {
            coupon = couponService.getValidCoupon(request.getCouponCode(), subtotal);

            // Tách logic tính discount
            if ("FREE_SHIPPING".equalsIgnoreCase(coupon.getDiscountType())) {
                // Đã fix lỗi toán học: Chỉ lấy discount bằng đúng số tiền ship, không set shippingFee = 0
                discount = shippingFee;
            } else {
                discount = couponService.calculateDiscount(coupon, subtotal);
            }
        }

        // 7. Payment
        PaymentMethod paymentMethod = getPaymentMethod(request.getPaymentMethodId());

        // 8. Total
        BigDecimal total = subtotal
                .add(shippingFee)
                .subtract(discount)
                .max(BigDecimal.ZERO);

        // 9. Response
        return CheckoutPreviewResponse.builder()
                .items(itemResponses)
                .subtotal(subtotal)
                .shippingFee(shippingFee)
                .discountAmount(discount)
                .totalAmount(total)
                .shippingMethod(shippingMethodMapper.toResponse(shippingMethod))
                .address(userAddressMapper.toResponse(address))
                .coupon(coupon != null ? couponMapper.toResponse(coupon) : null)
                .paymentMethodCode(paymentMethod.getCode())
                .build();
    }

    private Cart getCart(Integer userId) {
        return cartRepository.findByUserId(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CART_NOT_FOUND));
    }

    private BigDecimal calculateSubtotal(List<CartItem> items) {
        return items.stream()
                .map(item -> item.getProductSku().getPrice()
                        .multiply(BigDecimal.valueOf(item.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private ShippingMethod getShippingMethod(Integer id) {
        return shippingMethodRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SHIPPING_METHOD_NOT_FOUND));
    }

    private UserAddress getAddress(Integer userId, Integer addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ADDRESS_NOT_FOUND));

        if (!address.getUser().getId().equals(userId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        return address;
    }

    private boolean hasCoupon(CheckoutPreviewRequest request) {
        return request.getCouponCode() != null && !request.getCouponCode().trim().isEmpty();
    }

    private PaymentMethod getPaymentMethod(Integer id) {
        return paymentMethodRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PAYMENT_METHOD_NOT_FOUND));
    }

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        ProductSku sku = item.getProductSku();

        // TÁI SỬ DỤNG LOGIC MAP THUỘC TÍNH TỪ CART SERVICE
        List<SkuAttributeResponse> attributes = sku.getSkuValues() != null ?
                sku.getSkuValues().stream()
                        .map(skuValue -> {
                            var attrVal = skuValue.getAttributeValue();
                            var attr = attrVal.getAttribute();
                            return SkuAttributeResponse.builder()
                                    .attributeId(attr.getId())
                                    .attributeName(attr.getName())
                                    .valueId(attrVal.getId())
                                    .valueName(attrVal.getValue())
                                    .build();
                        }).collect(Collectors.toList()) : null;

        return CartItemResponse.builder()
                .id(item.getId())
                .skuId(sku.getId())
                .productName(sku.getProduct().getName())
                .imageUrl(helper.resolveSkuImage(sku))
                .quantity(item.getQuantity())
                .price(sku.getPrice())
                .attributeValues(attributes) // Bổ sung để FE hiện Size, Màu
                .build();
    }

}