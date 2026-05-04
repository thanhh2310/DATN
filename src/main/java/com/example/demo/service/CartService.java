package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.*;
import com.example.demo.dto.response.*;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final CartItemRepository cartItemRepository;
    private final ProductSkuRepository skuRepository;
    private final UserRepository userRepository;
    private final Helper helper;

    public Cart getOrCreateCart(CartCreationRequest request) {
        if (request.getUserId() == null && (request.getSessionId() == null || request.getSessionId().isBlank())) {
            String sessionId = generateSessionId();

            return cartRepository.save(
                    Cart.builder()
                            .sessionId(sessionId)
                            .build()
            );
        }

        if (request.getUserId() != null) {
            return cartRepository.findByUserId(request.getUserId())
                    .orElseGet(() -> {
                        User user = userRepository.findById(request.getUserId())
                                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
                        return cartRepository.save(Cart.builder().user(user).build());
                    });
        }

        return cartRepository.findBySessionId(request.getSessionId())
                .orElseGet(() -> cartRepository.save(Cart.builder().sessionId(request.getSessionId()).build()));
    }

    public String generateSessionId() {
        return UUID.randomUUID().toString();
    }

    @Transactional
    public CartDetailResponse addItem(CartCreationRequest cartReq, CartItemRequest itemReq) {

        if (itemReq.getQuantity() == null || itemReq.getQuantity() <= 0) {
            throw new WebErrorConfig(ErrorCode.INVALID_QUANTITY);
        }

        Cart cart = getOrCreateCart(cartReq);

        ProductSku sku = skuRepository.findById(itemReq.getSkuId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        if (!sku.getIsActive()) {
            throw new WebErrorConfig(ErrorCode.SKU_INACTIVE);
        }

        if (itemReq.getQuantity() > sku.getStockQuantity()) {
            throw new WebErrorConfig(ErrorCode.INSUFFICIENT_STOCK);
        }

        Optional<CartItem> existing = cartItemRepository
                .findByCartIdAndProductSkuId(cart.getId(), sku.getId());

        if (existing.isPresent()) {
            CartItem item = existing.get();
            int newQty = item.getQuantity() + itemReq.getQuantity();

            if (newQty > sku.getStockQuantity()) {
                throw new WebErrorConfig(ErrorCode.INSUFFICIENT_STOCK);
            }
            item.setQuantity(newQty);
        } else {
            cartItemRepository.save(
                    CartItem.builder()
                            .cart(cart)
                            .productSku(sku)
                            .quantity(itemReq.getQuantity())
                            .build()
            );
        }

        CartCreationRequest newReq = CartCreationRequest.builder()
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .build();

        return getCartDetail(newReq);
    }

    @Transactional
    public CartDetailResponse updateItem(CartCreationRequest cartReq, Integer itemId, CartItemUpdateRequest request) {
        if (request.getQuantity() == null || request.getQuantity() <= 0) {
            return removeItem(cartReq, itemId);
        }

        Cart cart = getOrCreateCart(cartReq);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CART_ITEM_NOT_FOUND));

        // CHECK QUYỀN SỞ HỮU (IDOR Prevention)
        if (!item.getCart().getId().equals(cart.getId())) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_CART_ACCESS);
        }

        ProductSku sku = item.getProductSku();
        if (request.getQuantity() > sku.getStockQuantity()) {
            throw new WebErrorConfig(ErrorCode.INSUFFICIENT_STOCK);
        }

        item.setQuantity(request.getQuantity());
        cartItemRepository.save(item);

        CartCreationRequest newReq = CartCreationRequest.builder()
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .build();

        return getCartDetail(newReq);
    }


    @Transactional
    public CartDetailResponse removeItem(CartCreationRequest cartReq, Integer itemId) {
        Cart cart = getOrCreateCart(cartReq);
        CartItem item = cartItemRepository.findById(itemId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CART_ITEM_NOT_FOUND));

        if (!item.getCart().getId().equals(cart.getId())) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_CART_ACCESS);
        }

        cartItemRepository.deleteById(itemId);
        // Trả về giỏ hàng mới nhất

        CartCreationRequest newReq = CartCreationRequest.builder()
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .build();
        return getCartDetail(newReq);
    }

    @Transactional
    public CartDetailResponse clearCart(CartCreationRequest request) {
        Cart cart = getOrCreateCart(request);
        cartItemRepository.deleteByCartId(cart.getId());

        CartCreationRequest newReq = CartCreationRequest.builder()
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .build();

        return getCartDetail(newReq);
    }

    @Transactional
    public CartDetailResponse mergeCart(String sessionId, Integer userId) {
        if (sessionId == null || userId == null) throw new WebErrorConfig(ErrorCode.MISSING_CART_IDENTIFIER);

//        Optional<Cart> guestOpt = cartRepository.findBySessionId(sessionId);
//        if (guestOpt.isEmpty()) return;
//
//        Cart guestCart = guestOpt.get();
        Cart userCart = cartRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
                    return cartRepository.save(Cart.builder().user(user).build());
                });

        Optional<Cart> guestOpt = cartRepository.findBySessionId(sessionId);
        if (guestOpt.isEmpty()) {
            return getCartDetail(CartCreationRequest.builder().userId(userId).build());
        }

        Cart guestCart = guestOpt.get();

        List<CartItem> guestItems = cartItemRepository.findByCartId(guestCart.getId());

        for (CartItem guestItem : guestItems) {
            Integer skuId = guestItem.getProductSku().getId();
            Optional<CartItem> userItemOpt = cartItemRepository.findByCartIdAndProductSkuId(userCart.getId(), skuId);

            if (userItemOpt.isPresent()) {
                CartItem userItem = userItemOpt.get();
                int newQty = userItem.getQuantity() + guestItem.getQuantity();
                int stock = guestItem.getProductSku().getStockQuantity();
                userItem.setQuantity(Math.min(newQty, stock));
                cartItemRepository.delete(guestItem); // Xóa item cũ bên guest
            } else {
                guestItem.setCart(userCart);
                cartItemRepository.save(guestItem); // Lưu lại item đã đổi giỏ
            }
        }
        cartItemRepository.flush();
        cartRepository.delete(guestCart); // Xóa giỏ guest

        return getCartDetail(CartCreationRequest.builder().userId(userId).build());
    }


    public CartDetailResponse getCartDetail(CartCreationRequest request) {
        Cart cart = getOrCreateCart(request);
        List<CartItem> items = cartItemRepository.findByCartId(cart.getId());

        List<CartItemResponse> responses = new ArrayList<>();
        BigDecimal total = BigDecimal.ZERO;

        for (CartItem item : items) {
            ProductSku sku = item.getProductSku();
            BigDecimal price = sku.getPrice();
            total = total.add(price.multiply(BigDecimal.valueOf(item.getQuantity())));

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
                            }).collect(Collectors.toList()) : null;

            responses.add(CartItemResponse.builder()
                    .id(item.getId())
                    .skuId(sku.getId())
                    .productName(sku.getProduct().getName())
                    .imageUrl(helper.resolveSkuImage(sku))
                    .quantity(item.getQuantity())
                    .price(price)
                    .attributeValues(attributeValues)
                    .build());
        }

        return CartDetailResponse.builder()
                .id(cart.getId())
                .userId(cart.getUser() != null ? cart.getUser().getId() : null)
                .sessionId(cart.getSessionId())
                .createdAt(cart.getCreatedAt())
                .updatedAt(cart.getUpdatedAt())
                .items(responses)
                .totalAmount(total)
                .build();
    }


}