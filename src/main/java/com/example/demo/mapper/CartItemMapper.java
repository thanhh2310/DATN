package com.example.demo.mapper;

import com.example.demo.dto.response.CartItemResponse;
import com.example.demo.model.CartItem;
import com.example.demo.model.ProductSku;
import com.example.demo.service.Helper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class CartItemMapper {
    private final Helper helper;

    private CartItemResponse mapToCartItemResponse(CartItem item) {
        ProductSku sku = item.getProductSku();

        return CartItemResponse.builder()
                .id(item.getId())
                .skuId(sku.getId())
                .productName(sku.getProduct().getName())
                .quantity(item.getQuantity())
                .price(sku.getPrice())
                .imageUrl(helper.resolveSkuImage(sku))
                .build();
    }


}
