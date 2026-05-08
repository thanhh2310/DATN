package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.CategoryCreationRequest;
import com.example.demo.dto.request.CategoryUpdateRequest;
import com.example.demo.dto.response.CategoryResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.mapper.CategoryMapper;
import com.example.demo.model.Category;
import com.example.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class  CategoryService {
    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;
    private final Helper helper;

    @Transactional
    public CategoryResponse createCategory(CategoryCreationRequest request) {
        // 2. Map dữ liệu cơ bản từ request sang Entity
        Category category = categoryMapper.requestToCategory(request);

        // --- XỬ LÝ SLUG
        if((request.getSlug() != null) &&(!request.getSlug().isEmpty()) ){
            category.setSlug(request.getSlug());
        } else {
            category.setSlug(helper.generateUniqueSlug(request.getName()));
        }

        if (categoryRepository.existsBySlug(request.getSlug())) {
            throw new WebErrorConfig(ErrorCode.CATEGORY_ALREADY_EXISTED);
        }

        // 3. Xử lý logic gán Danh mục cha (nếu có)
        if (request.getParentId() != null) {
            Category parent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));
            category.setParent(parent);
        }

        // 4. Lưu vào Database
        category = categoryRepository.save(category);

        // 5. Map sang Response DTO và trả về
        return categoryMapper.categoryToResponse(category);
    }

    @Transactional
    public CategoryResponse updateCategory(Integer id, CategoryUpdateRequest request) {
        // 1. Lấy danh mục hiện tại ra
        Category existingCategory = categoryRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

        // 2. Kiểm tra trùng lặp Slug (Chỉ lỗi nếu slug mới trùng với một Category KHÁC)
        if (request.getSlug() != null &&
                !request.getSlug().equals(existingCategory.getSlug()) &&
                categoryRepository.existsBySlug(request.getSlug())) {
            throw new WebErrorConfig(ErrorCode.CATEGORY_ALREADY_EXISTED);
        }

        // 3. Cập nhật các field cơ bản qua Mapper
        categoryMapper.updateToCategory(request, existingCategory);

        // 4. Xử lý cập nhật Danh mục cha
        if (request.getParentId() == null) {
            existingCategory.setParent(null); // Chuyển thành danh mục gốc
        } else {
            // Ngăn chặn việc gán chính nó làm cha của nó
            if (request.getParentId().equals(id)) {
                throw new WebErrorConfig(ErrorCode.INVALID_CATEGORY_PARENT); // Cần định nghĩa lỗi này
            }

            Category newParent = categoryRepository.findById(request.getParentId())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

            // Logic cấp cao: Kiểm tra vòng lặp đệ quy (Cycle Dependency)
            // Ví dụ: A -> B -> C. Không thể set cha của A là C.
            if (isChildCategory(existingCategory, newParent.getId())) {
                throw new WebErrorConfig(ErrorCode.CATEGORY_CYCLE_DETECTED); // Cần định nghĩa lỗi này
            }

            existingCategory.setParent(newParent);
        }

        // 5. Lưu và trả về kết quả
        existingCategory = categoryRepository.save(existingCategory);
        return categoryMapper.categoryToResponse(existingCategory);
    }

    @Transactional(readOnly = true)
    public CategoryResponse getCategoryById(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

        return categoryMapper.categoryToResponse(category);
    }

    @Transactional(readOnly = true)
    public PageResponse<CategoryResponse> getAllCategories(Pageable pageable) {

        Page<Category> page = categoryRepository.findAll(pageable);

        return PageResponse.<CategoryResponse>builder()
                .currentPage(page.getNumber())
                .totalPage(page.getTotalPages())
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .items(page.getContent().stream()
                        .map(categoryMapper::categoryToResponse)
                        .toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<CategoryResponse> getCategoryTree() {
        // Lấy tất cả các danh mục có parent_id = null (Danh mục gốc)
        List<Category> rootCategories = categoryRepository.findByParentIsNull();

        // Vì trong CategoryMapper chúng ta đã viết logic đệ quy map subCategories,
        // Nên chỉ cần truyền list gốc vào, mapper sẽ tự động duyệt hết cây.
        return rootCategories.stream()
                .map(categoryMapper::categoryToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteCategory(Integer id) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));
        Category parent = category.getParent();
        List<Category> subCategories = new ArrayList<>(category.getSubCategories());

        for (Category sub : subCategories) {
            sub.setParent(parent);

            if (parent != null) {
                parent.getSubCategories().add(sub);
            }
        }
        category.getSubCategories().clear();

        categoryRepository.saveAll(subCategories);
        categoryRepository.delete(category);
    }

    private boolean isChildCategory(Category currentCategory, Integer targetParentId) {
        if (currentCategory.getSubCategories() == null || currentCategory.getSubCategories().isEmpty()) {
            return false;
        }

        for (Category subCategory : currentCategory.getSubCategories()) {
            if (subCategory.getId().equals(targetParentId)) {
                return true;
            }
            // Đệ quy check tiếp các đời cháu
            if (isChildCategory(subCategory, targetParentId)) {
                return true;
            }
        }
        return false;
    }

}