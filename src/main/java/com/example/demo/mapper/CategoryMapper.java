package com.example.demo.mapper;

import com.example.demo.dto.request.CategoryCreationRequest;
import com.example.demo.dto.request.CategoryUpdateRequest;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.model.Category;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.stream.Collectors;

@Component
public class CategoryMapper {

    /**
     * Chuyển đổi từ Creation Request sang Entity
     */
    public Category requestToCategory(CategoryCreationRequest request) {
        if (request == null) {
            return null;
        }

        Category category = new Category();
        category.setName(request.getName());
        category.setSlug(request.getSlug());
        category.setImageUrl(request.getImageUrl());

        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        // Lưu ý: Việc lấy `parentId` từ request để truy vấn Database tìm Category cha
        // và gọi category.setParent(...) NÊN ĐƯỢC XỬ LÝ Ở TẦNG SERVICE.
        return category;
    }

    /**
     * Chuyển đổi từ Entity sang Response DTO (Có đệ quy sub-categories)
     */
    public CategoryResponse categoryToResponse(Category category) {
        if (category == null) {
            return null;
        }

        CategoryResponse.CategoryResponseBuilder response = CategoryResponse.builder()
                .id(category.getId())
                .name(category.getName())
                .slug(category.getSlug())
                .imageUrl(category.getImageUrl())
                .isActive(category.getIsActive())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt());

        // Map thông tin parentId (nếu có)
        if (category.getParent() != null) {
            response.parentId(category.getParent().getId());
        }

        // Đệ quy map danh sách các danh mục con (nếu có)
        if (category.getSubCategories() != null && !category.getSubCategories().isEmpty()) {
            response.subCategories(
                    category.getSubCategories().stream()
                            .map(this::categoryToResponse) // Gọi đệ quy lại chính hàm này
                            .collect(Collectors.toList())
            );
        } else {
            response.subCategories(Collections.emptyList());
        }

        return response.build();
    }

    /**
     * Cập nhật dữ liệu từ Update Request vào Entity hiện tại có sẵn trong DB
     */
    public void updateToCategory(CategoryUpdateRequest request, Category category) {
        if (request == null || category == null) {
            return;
        }

        if (request.getName() != null) {
            category.setName(request.getName());
        }
        if (request.getSlug() != null) {
            category.setSlug(request.getSlug());
        }
        if (request.getImageUrl() != null) {
            category.setImageUrl(request.getImageUrl());
        }
        if (request.getIsActive() != null) {
            category.setIsActive(request.getIsActive());
        }

        // Lưu ý: Tương tự như Create, logic check valid parentId và cập nhật Category cha
        // nên đẩy lên tầng Service để handle các lỗi như gán cha vào chính nó (Vòng lặp).
    }
}