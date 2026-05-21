package com.example.demo.mapper;

import com.example.demo.dto.response.SkuAttributeResponse;
import com.example.demo.dto.response.SkuImageResponse;
import com.example.demo.dto.response.SkuResponse;
import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.ProductSku;
import com.example.demo.model.Wishlist;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class WishlistMapper {
    public WishlistResponse toResponse(Wishlist wishlist) {
        Product product = wishlist.getProduct();

        // Lấy ảnh đầu tiên của sản phẩm (thumbnail)
        String imageUrl = product.getImages() != null
                ? product.getImages().stream()
                .findFirst()
                .map(ProductImage::getImageUrl)
                .orElse(null)
                : null;

        return WishlistResponse.builder()
                .id(wishlist.getId())
                .productId(product.getId())
                .productName(product.getName())
                .productSlug(product.getSlug())
                .productBasePrice(product.getBasePrice())
                .productImage(imageUrl)
                .brandName(product.getBrand() != null ? product.getBrand().getName() : null)
                .categoryName(product.getCategory() != null ? product.getCategory().getName() : null)
                .productIsActive(product.getIsActive())
                .skus(product.getSkus() != null
                        ? product.getSkus().stream()
                        .map(this::mapSku)
                        .collect(Collectors.toList())
                        : List.of())
                .addedAt(wishlist.getCreatedAt())
                .build();
    }

    private SkuResponse mapSku(ProductSku sku) {
        return SkuResponse.builder()
                .id(sku.getId())
                .skuCode(sku.getSkuCode())
                .price(sku.getPrice())
                .stockQuantity(sku.getStockQuantity())
                .images(sku.getImages() != null
                        ? sku.getImages().stream()
                        .sorted(Comparator.comparing(image -> image.getDisplayOrder() != null ? image.getDisplayOrder() : 0))
                        .map(image -> SkuImageResponse.builder()
                                .id(image.getId())
                                .imageUrl(image.getImageUrl())
                                .isThumbnail(image.getIsThumbnail())
                                .displayOrder(image.getDisplayOrder())
                                .build())
                        .collect(Collectors.toList())
                        : List.of())
                .isActive(sku.getIsActive())
                .attributeValues(sku.getSkuValues() != null
                        ? sku.getSkuValues().stream()
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
                        .collect(Collectors.toList())
                        : List.of())
                .build();
    }
}
