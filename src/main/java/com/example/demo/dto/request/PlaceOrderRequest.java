package com.example.demo.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PlaceOrderRequest {
    @NotEmpty(message = "Danh sách cart item không được để trống")
    private List<Integer> cartItemIds;

    @NotNull(message = "Địa chỉ giao hàng không được để trống")
    private Integer addressId;       // địa chỉ giao hàng

    @NotNull(message = "Phương thức giao hàng không được để trống")
    private Integer shippingMethodId;

    private String couponCode;

    @NotNull(message = "Phương thức thanh toán không được để trống")
    private Integer paymentMethodId;
}
