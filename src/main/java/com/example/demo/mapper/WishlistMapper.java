package com.example.demo.mapper;

import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.Wishlist;
import org.springframework.stereotype.Component;

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
                .addedAt(wishlist.getCreatedAt())
                .build();
    }
}
