package com.example.demo.repository;

import com.example.demo.model.Order;
import com.example.demo.model.PaymentMethod;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@UtilityClass
public class OrderSpecification {

    public static Specification<Order> buildFilter(
            Integer orderId,
            String paymentMethodCode,
            BigDecimal minTotal,
            BigDecimal maxTotal
    ) {

        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (orderId != null) {
                predicates.add(cb.equal(root.get("id"), orderId));
            }

            if (paymentMethodCode != null && !paymentMethodCode.isBlank()) {
                Join<Order, PaymentMethod> pmJoin = root.join("paymentMethod", JoinType.LEFT);

                predicates.add(cb.equal(
                        cb.lower(pmJoin.get("code").as(String.class)),
                        paymentMethodCode.trim().toLowerCase()
                ));
            }

            if (minTotal != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("totalAmount"), minTotal));
            }

            if (maxTotal != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("totalAmount"), maxTotal));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}