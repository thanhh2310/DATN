package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderHistoryResponse {
    Integer orderId;
    Integer buyerId;
    String buyerName;
    String buyerEmail;
    String buyerPhone;

    Integer paymentMethodId;
    String paymentMethodCode;
    String paymentMethodName;
    String paymentProviderCode;
    String paymentTransactionId;

    String shippingAddress;
    String shippingCity;
    Integer shippingMethodId;
    String shippingMethodName;
    BigDecimal subtotal;
    BigDecimal shippingFee;
    BigDecimal discountAmount;
    Integer couponId;
    String couponCode;
    String couponDiscountType;
    BigDecimal couponDiscountValue;

    BigDecimal totalAmount;
    String orderStatus;
    String paymentStatus;
    LocalDateTime createdAt;

    List<OrderItemPreviewResponse> items;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @FieldDefaults(level = AccessLevel.PRIVATE)
    public static class OrderItemPreviewResponse {
        Integer skuId;
        Integer orderItemId;
        Integer productId;
        String productName;
        String productSlug;
        String skuCode;
        String imageUrl;
        Integer quantity;
        BigDecimal price;
        BigDecimal discount;
        List<SkuAttributeResponse> attributeValues;
    }
}
