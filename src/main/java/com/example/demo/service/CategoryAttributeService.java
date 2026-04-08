package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.CategoryAttributeRequest;
import com.example.demo.dto.response.CategoryAttributeResponse;
import com.example.demo.mapper.CategoryAttributeMapper;
import com.example.demo.model.Attribute;
import com.example.demo.model.Category;
import com.example.demo.model.CategoryAttribute;
import com.example.demo.repository.AttributeRepository;
import com.example.demo.repository.CategoryAttributeRepository;
import com.example.demo.repository.CategoryRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class CategoryAttributeService {
    private final CategoryAttributeRepository categoryAttributeRepository;
    private final CategoryRepository categoryRepository;
    private final AttributeRepository attributeRepository;
    private final CategoryAttributeMapper categoryAttributeMapper;

    @Transactional
    public CategoryAttributeResponse assignAttributeToCategory(CategoryAttributeRequest request) {
        if (categoryAttributeRepository.existsByCategoryIdAndAttributeId(
                request.getCategoryId(), request.getAttributeId())) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_ALREADY_EXISTED);
            // Better to have specific CATEGORY_ATTRIBUTE_EXISTS, but we reuse for now
        }

        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

        Attribute attribute = attributeRepository.findById(request.getAttributeId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        CategoryAttribute categoryAttribute = CategoryAttribute.builder()
                .category(category)
                .attribute(attribute)
                .isRequired(request.getIsRequired())
                .isFilterable(request.getIsFilterable())
                .build();

        categoryAttribute = categoryAttributeRepository.save(categoryAttribute);

        return categoryAttributeMapper.mapToResponse(categoryAttribute);
    }

    @Transactional(readOnly = true)
    public List<CategoryAttributeResponse> getAttributesByCategoryId(Integer categoryId) {
        if (!categoryRepository.existsById(categoryId)) {
            throw new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND);
        }

        List<CategoryAttribute> categoryAttributes =
                categoryAttributeRepository.findByCategoryId(categoryId);

        if (categoryAttributes.isEmpty()) {
            return Collections.emptyList(); // hoặc throw nếu bạn muốn
        }

        // 4. Map sang response
        return categoryAttributes.stream()
                .map(categoryAttributeMapper::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public CategoryAttributeResponse updateCategoryAttributeSettings(Integer id, CategoryAttributeRequest request) {
        CategoryAttribute categoryAttribute = categoryAttributeRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));

        if (request.getIsRequired() != null) {
            categoryAttribute.setIsRequired(request.getIsRequired());
        }
        if (request.getIsFilterable() != null) {
            categoryAttribute.setIsFilterable(request.getIsFilterable());
        }

        categoryAttribute = categoryAttributeRepository.save(categoryAttribute);

        return categoryAttributeMapper.mapToResponse(categoryAttribute);
    }

    @Transactional
    public void removeCategoryAttribute(Integer id) {
        if (!categoryAttributeRepository.existsById(id)) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND);
        }
        categoryAttributeRepository.deleteById(id);
    }


}
