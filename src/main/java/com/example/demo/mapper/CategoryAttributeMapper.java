package com.example.demo.mapper;

import com.example.demo.dto.response.CategoryAttributeResponse;
import com.example.demo.model.CategoryAttribute;
import org.springframework.stereotype.Component;

@Component
public class CategoryAttributeMapper {
    public CategoryAttributeResponse mapToResponse(CategoryAttribute entity) {
        return CategoryAttributeResponse.builder()
                .id(entity.getId())
                .categoryId(entity.getCategory().getId())
                .attributeId(entity.getAttribute().getId())
                .attributeName(entity.getAttribute().getName())
                .isRequired(entity.getIsRequired())
                .isFilterable(entity.getIsFilterable())
                .build();
    }
}
