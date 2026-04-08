package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CategoryResponse {
    Integer id;
    Integer parentId;
    String name;
    String slug;
    String imageUrl;
    Boolean isActive;
    LocalDateTime createdAt;
    LocalDateTime updatedAt;
    List<CategoryResponse> subCategories;
}