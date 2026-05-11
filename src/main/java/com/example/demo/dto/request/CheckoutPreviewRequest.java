package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CheckoutPreviewRequest {
    private Integer userId;          // hoặc lấy từ SecurityContext
    private String sessionId;        // cho guest
    private List<Integer> cartItemIds;
    private Integer addressId;       // địa chỉ giao hàng
    private Integer shippingMethodId;
    private String couponCode;
    private Integer paymentMethodId;
}
