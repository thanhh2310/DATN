package com.example.demo.model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.FieldDefaults;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "products")
@EntityListeners(AuditingEntityListener.class)
@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    Integer id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id")
    Category category;

    // Giả định bạn đã có class Brand tương tự Category
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "brand_id")
    Brand brand;

    @Column(nullable = false)
    String name;

    @Column(nullable = false, unique = true)
    String slug;

    // Lưu nội dung để AI đọc và Embedding
    @Column(columnDefinition = "TEXT")
    String description;

    @Column(name = "base_price", nullable = false)
    BigDecimal basePrice;

    @Builder.Default
    @Column(name = "is_active")
    Boolean isActive = true;

    @CreatedDate
    @Column(name = "created_at", updatable = false)
    LocalDateTime createdAt;

    @LastModifiedDate
    @Column(name = "updated_at")
    LocalDateTime updatedAt;

    // Thêm list này vào dưới cùng của class Product
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("displayOrder ASC") // Tự động sắp xếp ảnh theo thứ tự khi query
    Set<ProductImage> images = new HashSet<>();

    // THÊM MỚI: Móc nối 1-N tới bảng product_specs
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<ProductSpec> specs = new HashSet<>();

    // THÊM MỚI: Móc nối 1-N tới bảng product_skus
    @Builder.Default
    @OneToMany(mappedBy = "product", cascade = CascadeType.ALL, orphanRemoval = true)
    Set<ProductSku> skus = new HashSet<>();
}