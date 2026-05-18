package com.example.demo.projection;

import java.math.BigDecimal;
import java.time.LocalDate;

public interface DailyRevenueProjection {
    LocalDate getReportDate();
    Integer getTotalOrders();
    BigDecimal getTotalRevenue();
}