package com.example.demo.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutPreviewResponse {

    private List<CartItemResponse> items;

    private BigDecimal subtotal;     // tổng tiền sản phẩm
    private BigDecimal shippingFee;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;

    private UserAddressResponse address;
    private ShippingMethodResponse shippingMethod;
    private CouponResponse coupon;
    private String paymentMethodCode;

}
