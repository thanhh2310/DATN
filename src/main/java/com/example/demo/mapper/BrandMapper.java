package com.example.demo.mapper;

import com.example.demo.dto.request.BrandRequest;
import com.example.demo.dto.response.BrandResponse;
import com.example.demo.model.Brand;
import org.springframework.stereotype.Component;

@Component
public class BrandMapper {

    public Brand toBrand(BrandRequest request) {
        return Brand.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .logoUrl(request.getLogoUrl())
                .description(request.getDescription())
                .isActive(request.getIsActive())
                .build();
    }

    public BrandResponse toBrandResponse(Brand brand) {
        if (brand == null) return null;
        return BrandResponse.builder()
                .id(brand.getId())
                .name(brand.getName())
                .slug(brand.getSlug())
                .logoUrl(brand.getLogoUrl())
                .description(brand.getDescription())
                .isActive(brand.getIsActive())
                .build();
    }

    public void updateBrandFromRequest(Brand brand, BrandRequest request) {
        if (request.getName() != null) brand.setName(request.getName());
        if (request.getSlug() != null) brand.setSlug(request.getSlug());
        if (request.getLogoUrl() != null) brand.setLogoUrl(request.getLogoUrl());
        if (request.getDescription() != null) brand.setDescription(request.getDescription());
        if (request.getIsActive() != null) brand.setIsActive(request.getIsActive());
    }
}