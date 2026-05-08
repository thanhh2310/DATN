package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.SkuRequest;
import com.example.demo.dto.request.SkuStockUpdateRequest;
import com.example.demo.dto.response.SkuAttributeResponse;
import com.example.demo.dto.response.SkuDetailResponse;
import com.example.demo.dto.response.SkuImageResponse;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuService {

    private final ProductSkuRepository productSkuRepository;
    private final ProductSkuImageRepository productSkuImageRepository;
    private final ProductRepository productRepository;
    private final AttributeValueRepository attributeValueRepository;

    @Transactional
    public SkuDetailResponse createSku(Integer productId, SkuRequest request) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        if (productSkuRepository.existsBySkuCode(request.getSkuCode())) {
            throw new WebErrorConfig(ErrorCode.SKU_CODE_ALREADY_EXISTED);
        }

        ProductSku sku = ProductSku.builder()
                .product(product)
                .skuCode(request.getSkuCode())
                .price(request.getPrice())
                .stockQuantity(request.getStockQuantity())
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .skuValues(new java.util.HashSet<>())
                .images(new java.util.HashSet<>())
                .build();

        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                sku.getImages().add(ProductSkuImage.builder()
                        .productSku(sku)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .displayOrder(i)
                        .build());
            }
        }

        if (request.getAttributeValueIds() != null && !request.getAttributeValueIds().isEmpty()) {
            List<AttributeValue> attrValues = attributeValueRepository.findAllById(request.getAttributeValueIds());
            for (AttributeValue attrValue : attrValues) {
                SkuValue skuValue = SkuValue.builder()
                        .productSku(sku)
                        .attributeValue(attrValue)
                        .build();
                sku.getSkuValues().add(skuValue);
            }
        }

        productSkuRepository.save(sku);

        return mapToSkuDetailResponse(sku);
    }

    @Transactional
    public SkuDetailResponse updateSku(Integer skuId, SkuRequest request) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        if (!sku.getSkuCode().equals(request.getSkuCode())) {
            if (productSkuRepository.existsBySkuCode(request.getSkuCode())) {
                throw new WebErrorConfig(ErrorCode.SKU_CODE_ALREADY_EXISTED);
            }
            sku.setSkuCode(request.getSkuCode());
        }

        sku.setPrice(request.getPrice());
        sku.setStockQuantity(request.getStockQuantity());
        if (request.getIsActive() != null) {
            sku.setIsActive(request.getIsActive());
        }

        if (request.getImageUrls() != null) {
            sku.getImages().clear();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                sku.getImages().add(ProductSkuImage.builder()
                        .productSku(sku)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .displayOrder(i)
                        .build());
            }
        }

        if (request.getAttributeValueIds() != null) {
            sku.getSkuValues().clear();
            List<AttributeValue> attrValues = attributeValueRepository.findAllById(request.getAttributeValueIds());
            for (AttributeValue attrValue : attrValues) {
                SkuValue skuValue = SkuValue.builder()
                        .productSku(sku)
                        .attributeValue(attrValue)
                        .build();
                sku.getSkuValues().add(skuValue);
            }
        }

        return mapToSkuDetailResponse(sku);
    }

    @Transactional
    public SkuDetailResponse updateStock(Integer skuId, SkuStockUpdateRequest request) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        sku.setStockQuantity(request.getStockQuantity());

        return mapToSkuDetailResponse(sku);
    }

    @Transactional(readOnly = true)
    public SkuDetailResponse getSkuById(Integer skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        return mapToSkuDetailResponse(sku);
    }

    @Transactional(readOnly = true)
    public List<SkuDetailResponse> getSkusByProductId(Integer productId) {
        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        return product.getSkus().stream()
                .map(this::mapToSkuDetailResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteSku(Integer skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        sku.setIsActive(false);
    }

    private SkuDetailResponse mapToSkuDetailResponse(ProductSku sku) {
        List<SkuImageResponse> images = sku.getImages() != null ?
                sku.getImages().stream()
                        .sorted(Comparator.comparingInt(img -> img.getDisplayOrder() != null ? img.getDisplayOrder() : 0))
                        .map(img -> SkuImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .isThumbnail(img.getIsThumbnail())
                                .displayOrder(img.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList()) : null;

        List<SkuAttributeResponse> attributeValues = sku.getSkuValues() != null ?
                sku.getSkuValues().stream()
                        .map(skuValue -> {
                            var attrVal = skuValue.getAttributeValue();
                            var attr = attrVal.getAttribute();
                            return SkuAttributeResponse.builder()
                                    .attributeId(attr.getId())
                                    .attributeName(attr.getName())
                                    .valueId(attrVal.getId())
                                    .valueName(attrVal.getValue())
                                    .description(attrVal.getDescription())
                                    .build();
                        })
                        .collect(Collectors.toList()) : null;

        return SkuDetailResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .stockQuantity(sku.getStockQuantity())
                .isActive(sku.getIsActive())
                .createdAt(sku.getCreatedAt())
                .images(images)
                .attributeValues(attributeValues)
                .build();
    }
}
