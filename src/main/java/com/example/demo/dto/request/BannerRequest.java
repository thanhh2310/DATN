package com.example.demo.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BannerRequest {
    private String title;

    @NotBlank(message = "Image url cannot be blank")
    private String imageUrl;

    private String targetUrl;

    private String position;

    private Integer displayOrder;

    private Boolean isActive;
}
