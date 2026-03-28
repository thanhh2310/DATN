package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.util.Collections;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PageResponse<T> {
    int currentPage;
    int totalPage;
    int pageSize;
    long totalElements;

    @Builder.Default
    List<T> data = Collections.emptyList();
}
