package com.example.demo.dto.request;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.experimental.FieldDefaults;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CouponRequest {

    @NotBlank(message = "Code must not be blank")
    @Size(max = 50, message = "Code must be <= 50 characters")
    String code;

    @NotBlank(message = "Discount type is required")
    @Pattern(
            regexp = "PERCENT|FIXED",
            message = "Discount type must be PERCENT or FIXED"
    )
    String discountType;

    @NotNull(message = "Discount value is required")
    @DecimalMin(value = "0.0", inclusive = true, message = "Discount must be >= 0")
    @DecimalMax(value = "100.0", inclusive = true, message = "Discount must be <= 100")
    BigDecimal discountValue;

    @DecimalMin(value = "0.0", inclusive = true, message = "Min order must be >= 0")
    BigDecimal minOrderValue;

    @Min(value = 1, message = "Usage limit must be >= 1")
    Integer usageLimit;

    LocalDateTime startDate;

    LocalDateTime endDate;

    Boolean isActive;
}
