package com.example.demo.mapper;

import com.example.demo.dto.response.*;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.ProductSku;
import com.example.demo.model.ProductSpec;
import com.example.demo.repository.OrderItemRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class ProductMapper {
    private final OrderItemRepository orderItemRepository;

    public ProductResponse toProductResponse(Product product) {
        if (product == null) return null;

        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .slug(product.getSlug())
                .description(product.getDescription())
                .basePrice(product.getBasePrice())
                .soldCount(orderItemRepository.countSoldByProductId(product.getId()))
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
                .attributeId(spec.getAttributeValue() != null && spec.getAttributeValue().getAttribute() != null
                        ? spec.getAttributeValue().getAttribute().getId() : null)
                .attributeName(spec.getAttributeValue() != null && spec.getAttributeValue().getAttribute() != null
                        ? spec.getAttributeValue().getAttribute().getName() : null)
                .attributeValueId(spec.getAttributeValue() != null ? spec.getAttributeValue().getId() : null)
                .attributeValue(spec.getAttributeValue() != null ? spec.getAttributeValue().getValue() : null)
                .build();
    }

    private SkuResponse mapSku(ProductSku sku) {
        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .stockQuantity(sku.getStockQuantity())
                .images(sku.getImages() != null ?
                        sku.getImages().stream()
                                .sorted((img1, img2) -> {
                                    Integer order1 = img1.getDisplayOrder() != null ? img1.getDisplayOrder() : 0;
                                    Integer order2 = img2.getDisplayOrder() != null ? img2.getDisplayOrder() : 0;
                                    return order1.compareTo(order2);
                                })
                                .map(skuImage -> SkuImageResponse.builder()
                                        .id(skuImage.getId())
                                        .imageUrl(skuImage.getImageUrl())
                                        .isThumbnail(skuImage.getIsThumbnail())
                                        .displayOrder(skuImage.getDisplayOrder())
                                        .build())
                                .collect(Collectors.toList()) : null)
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
                                            .description(attrVal.getDescription())
                                            .build();
                                })
                                .collect(Collectors.toList()) : null)
                .build();
    }
}
