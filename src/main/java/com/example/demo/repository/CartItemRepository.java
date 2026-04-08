package com.example.demo.repository;

import com.example.demo.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCartIdAndSkuId(Integer cartId, Integer skuId);
    List<CartItem> findByCartId(Integer cartId);
    void deleteByCartId(Integer cartId);
}
