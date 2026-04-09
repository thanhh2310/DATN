package com.example.demo.mapper;

import com.example.demo.dto.response.ShippingMethodResponse;
import com.example.demo.model.ShippingMethod;
import org.springframework.stereotype.Component;

@Component
public class ShippingMethodMapper {

    public ShippingMethodResponse toResponse(ShippingMethod method) {
        return ShippingMethodResponse.builder()
                .id(method.getId())
                .name(method.getName())
                .cost(method.getCost())
                .estimatedDeliveryDays(method.getEstimatedDeliveryDays())
                .build();
    }
}
