package com.example.demo.repository;

import com.example.demo.model.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CartItemRepository extends JpaRepository<CartItem, Integer> {
    Optional<CartItem> findByCartIdAndProductSkuId(Integer cartId, Integer skuId);
    List<CartItem> findByCartId(Integer cartId);
    void deleteByCartId(Integer cartId);

    @Query("SELECT ci FROM CartItem ci " +
            "JOIN FETCH ci.productSku sku " +
            "JOIN FETCH sku.product p " +
            "LEFT JOIN FETCH sku.skuValues sv " +
            "LEFT JOIN FETCH sv.attributeValue av " +
            "LEFT JOIN FETCH av.attribute " +
            "WHERE ci.cart.id = :cartId")
    List<CartItem> findByCartIdWithDetails(@Param("cartId") Integer cartId);

    @Query("SELECT ci FROM CartItem ci " +
            "JOIN FETCH ci.productSku sku " +
            "JOIN FETCH sku.product p " +
            "WHERE ci.cart.id = :cartId AND ci.id IN :itemIds")
    List<CartItem> findSelectedItems(@Param("cartId") Integer cartId, @Param("itemIds") List<Integer> itemIds);
}
