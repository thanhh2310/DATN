package com.example.demo.mapper;

import com.example.demo.dto.response.*;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.ProductSku;
import com.example.demo.model.ProductSpec;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
public class ProductMapper {

    public ProductResponse toProductResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .isActive(product.getIsActive())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())

                // Map Category an toàn (tránh NullPointer)
                .categoryId(product.getCategory() != null ? product.getCategory().getId() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)

                // Map Brand an toàn
                .brandId(product.getBrand() != null ? product.getBrand().getId() : null)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)

                // Map Images
                .images(product.getImages() != null ?
                        product.getImages().stream().map(this::mapImage).collect(Collectors.toList()) : null)

                // Map Specs
                .specs(product.getSpecs() != null ?
                        product.getSpecs().stream().map(this::mapSpec).collect(Collectors.toList()) : null)

                // Map SKUs
                .skus(product.getSkus() != null ?
                        product.getSkus().stream().map(this::mapSku).collect(Collectors.toList()) : null)
                .build();
    }

    // Các hàm helper nhỏ giúp code sạch sẽ hơn

    private ImageResponse mapImage(ProductImage image) {
        return ImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(image.getIsThumbnail())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    private SpecResponse mapSpec(ProductSpec spec) {
        return SpecResponse.builder()
                .id(spec.getId())
                // Lấy tên của thuộc tính (VD: Màn hình, Pin) từ bảng Attribute
                .attributeName(spec.getAttribute() != null ? spec.getAttribute().getName() : null)
                .value(spec.getValue())
                .build();
    }

    private SkuResponse mapSku(ProductSku sku) {
        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .stockQuantity(sku.getStockQuantity())
                .imageUrl(sku.getImageUrl())
                .isActive(sku.getIsActive())

                // Map sang class SkuAttributeResponse độc lập
                .attributeValues(sku.getSkuValues() != null ?
                        sku.getSkuValues().stream()
                                .map(skuValue -> {
                                    var attrVal = skuValue.getAttributeValue();
                                    var attr = attrVal.getAttribute();

                                    // Gọi trực tiếp Builder của class SkuAttributeResponse
                                    return SkuAttributeResponse.builder()
                                            .attributeId(attr.getId())
                                            .attributeName(attr.getName())
                                            .valueId(attrVal.getId())
                                            .valueName(attrVal.getValue())
                                            .build();
                                })
                                .collect(Collectors.toList()) : null)
                .build();
    }
}