package com.example.demo.mapper;

import com.example.demo.dto.response.UserAddressResponse;
import com.example.demo.model.UserAddress;
import org.springframework.stereotype.Component;

@Component
public class UserAddressMapper {
    public UserAddressResponse toResponse(UserAddress address) {

        return UserAddressResponse.builder()
                .id(address.getId())
                .userId(address.getUser().getId())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .city(address.getCity())
                .state(address.getState())
                .country(address.getCountry())
                .postalCode(address.getPostalCode())
                .isDefault(address.getIsDefault())
                .createdAt(address.getCreatedAt())
                .build();
    }
}
