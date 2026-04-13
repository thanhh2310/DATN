package com.example.demo.mapper;

import com.example.demo.dto.response.PaymentMethodResponse;
import com.example.demo.model.PaymentMethod;
import org.springframework.stereotype.Component;

@Component
public class PaymentMethodMapper {

    public PaymentMethodResponse toResponse(PaymentMethod method) {
        return PaymentMethodResponse.builder()
                .id(method.getId())
                .name(method.getName())
                .code(method.getCode())
                .isActive(method.getIsActive())
                .createdAt(method.getCreatedAt())
                .build();
    }
}