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

import java.util.*;
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
            validateUniqueIntegerIds(request.getAttributeValueIds());
            List<AttributeValue> attrValues = attributeValueRepository.findAllById(request.getAttributeValueIds());
            validateAllAttributeValuesExist(request.getAttributeValueIds(), attrValues);
            validateOneValuePerAttribute(attrValues);

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

        // 1. Cập nhật Core SKU
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

        // 2. Cập nhật Ảnh SKU
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

        // 3. Cập nhật SkuValues (SMART UPDATE ĐỂ FIX LỖI DUPLICATE KEY)
        if (request.getAttributeValueIds() != null) {
            validateUniqueIntegerIds(request.getAttributeValueIds());

            // Gom danh sách ID mới thành Set để query cho nhanh
            Set<Integer> newAttrValueIds = new HashSet<>(request.getAttributeValueIds());

            // A. XÓA những thuộc tính cũ không còn nằm trong request
            sku.getSkuValues().removeIf(sv -> !newAttrValueIds.contains(sv.getAttributeValue().getId()));

            // B. Lấy danh sách ID thuộc tính ĐANG CÓ để không Add đè lên
            Set<Integer> existingIds = sku.getSkuValues().stream()
                    .map(sv -> sv.getAttributeValue().getId())
                    .collect(Collectors.toSet());

            // C. Tải các AttributeValue từ DB lên để chuẩn bị Add
            Map<Integer, AttributeValue> attributeValueMap = attributeValueRepository.findAllById(newAttrValueIds)
                    .stream().collect(Collectors.toMap(AttributeValue::getId, av -> av));
            validateAllAttributeValuesExist(request.getAttributeValueIds(), new ArrayList<>(attributeValueMap.values()));
            validateOneValuePerAttribute(new ArrayList<>(attributeValueMap.values()));

            // D. THÊM MỚI những thuộc tính chưa có
            for (Integer newId : newAttrValueIds) {
                if (!existingIds.contains(newId)) {
                    AttributeValue attrValue = attributeValueMap.get(newId);
                    if (attrValue == null) {
                        throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
                    }
                    sku.getSkuValues().add(SkuValue.builder()
                            .productSku(sku)
                            .attributeValue(attrValue)
                            .build());
                }
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

    private void validateUniqueIntegerIds(List<Integer> ids) {
        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
        }

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new WebErrorConfig(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE_ID_IN_REQUEST);
        }
    }

    private void validateAllAttributeValuesExist(List<Integer> requestedIds, List<AttributeValue> attrValues) {
        if (attrValues.size() != new HashSet<>(requestedIds).size()) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
        }
    }

    private void validateOneValuePerAttribute(List<AttributeValue> attrValues) {
        Set<Integer> attributeIds = new HashSet<>();
        for (AttributeValue attrValue : attrValues) {
            if (!attributeIds.add(attrValue.getAttribute().getId())) {
                throw new WebErrorConfig(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE_IN_SPECS);
            }
        }
    }
}
