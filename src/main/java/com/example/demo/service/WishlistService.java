package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.WishlistResponse;
import com.example.demo.mapper.WishlistMapper;
import com.example.demo.model.Product;
import com.example.demo.model.ProductImage;
import com.example.demo.model.User;
import com.example.demo.model.Wishlist;
import com.example.demo.repository.ProductRepository;
import com.example.demo.repository.UserRepository;
import com.example.demo.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class WishlistService {

    private final WishlistRepository wishlistRepository;
    private final UserRepository userRepository;
    private final ProductRepository productRepository;
    private final WishlistMapper wishlistMapper;

    /**
     * Thêm sản phẩm vào wishlist
     */
    @Transactional
    public WishlistResponse addToWishlist(Integer userId, Integer productId) {
        // Kiểm tra sản phẩm đã có trong wishlist chưa
        if (wishlistRepository.existsByUserIdAndProductId(userId, productId)) {
            throw new WebErrorConfig(ErrorCode.WISHLIST_ITEM_ALREADY_EXISTED);
        }

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        Product product = productRepository.findById(productId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        Wishlist wishlist = Wishlist.builder()
                .user(user)
                .product(product)
                .build();

        wishlist = wishlistRepository.save(wishlist);

        return wishlistMapper.toResponse(wishlist);
    }

    /**
     * Xoá sản phẩm khỏi wishlist
     */
    @Transactional
    public void removeFromWishlist(Integer userId, Integer productId) {
        Wishlist wishlist = wishlistRepository.findByUserIdAndProductId(userId, productId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.WISHLIST_ITEM_NOT_FOUND));

        wishlistRepository.delete(wishlist);
    }

    /**
     * Lấy danh sách wishlist của user (có phân trang)
     */
    @Transactional(readOnly = true)
    public PageResponse<WishlistResponse> getWishlist(Integer userId, int page, int size) {
        Pageable pageable = PageRequest.of(page - 1, size, Sort.by("createdAt").descending());
        Page<Wishlist> wishlistPage = wishlistRepository.findByUserId(userId, pageable);

        return PageResponse.<WishlistResponse>builder()
                .currentPage(page)
                .totalPage(wishlistPage.getTotalPages())
                .pageSize(size)
                .totalElements(wishlistPage.getTotalElements())
                .items(wishlistPage.getContent().stream().map(wishlistMapper::toResponse).toList())
                .build();
    }

    /**
     * Kiểm tra sản phẩm có trong wishlist không
     */
    public boolean isInWishlist(Integer userId, Integer productId) {
        return wishlistRepository.existsByUserIdAndProductId(userId, productId);
    }


}
