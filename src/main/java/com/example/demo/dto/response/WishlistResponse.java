package com.example.demo.dto.response;

import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WishlistResponse {
    Integer id;
    Integer productId;
    String productName;
    String productSlug;
    BigDecimal productBasePrice;
    String productImage;
    String brandName;
    String categoryName;
    Boolean productIsActive;
    List<SkuResponse> skus;
    LocalDateTime addedAt;
}
