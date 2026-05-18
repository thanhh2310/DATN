package com.example.demo.projection;

import java.math.BigDecimal;

public interface TopProductProjection {
    Integer getProductId();
    String getProductName();
    Integer getTotalSold();
    BigDecimal getTotalRevenue();
}