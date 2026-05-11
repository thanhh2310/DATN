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
public class PlaceOrderRequest {
    private List<Integer> cartItemIds;
    private Integer addressId;       // địa chỉ giao hàng
    private Integer shippingMethodId;
    private String couponCode;
    private Integer paymentMethodId;
}
