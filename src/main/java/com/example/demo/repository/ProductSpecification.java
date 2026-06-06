package com.example.demo.repository;

import com.example.demo.dto.request.ProductFilterRequest;
import com.example.demo.model.Product;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.criteria.Subquery;
import lombok.experimental.UtilityClass;
import org.springframework.data.jpa.domain.Specification;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;

@UtilityClass
public class ProductSpecification {

    /**
     * Build Specification tổng hợp từ tất cả tiêu chí lọc.
     *
     * @param request          DTO chứa các tiêu chí lọc từ client
     * @param groupedAttrValues Map<attributeId, List<attributeValueId>> — được tính toán
     *                         sẵn từ Service layer bằng cách query DB trước.
     *                         Key   = attribute group (VD: 1 = Màu sắc, 2 = RAM)
     *                         Value = các value được chọn trong nhóm đó (VD: [Đỏ, Xanh])
     */
    public Specification<Product> buildFilter(
            ProductFilterRequest request,
            Map<Integer, List<Integer>> groupedAttrValues,
            Set<Integer> categoryIds) {

        // 1. Luôn luôn khởi tạo với điều kiện gốc (Sản phẩm phải đang Active)
        Specification<Product> spec = Specification.where(isActive());

        // 2. Nối thêm Category nếu có
        if (categoryIds != null && !categoryIds.isEmpty()) {
            spec = spec.and(hasCategoryIn(categoryIds));
        }

        // 3. Nối thêm Brand nếu có
        if (request.getBrandId() != null) {
            spec = spec.and(hasBrand(request.getBrandId()));
        }

        // 4. Nối thêm Price nếu có Min hoặc Max
        if (request.getMinPrice() != null || request.getMaxPrice() != null) {
            spec = spec.and(hasPriceBetween(request.getMinPrice(), request.getMaxPrice()));
        }

        // 5. Nối thêm từng nhóm Attribute
        if (groupedAttrValues != null) {
            for (List<Integer> idsInGroup : groupedAttrValues.values()) {
                if (idsInGroup != null && !idsInGroup.isEmpty()) {
                    spec = spec.and(hasAnyAttributeValueInGroup(idsInGroup));
                }
            }
        }

        // 6. Lọc tồn kho. Nếu có attributeValueIds, tồn kho được tính trên SKU khớp các thuộc tính đã chọn.
        // Ví dụ: Size M = 0, Size L = 32. Lọc OUT_OF_STOCK + Size M vẫn hiển thị sản phẩm.
        if (request.getStockFilter() != null && !request.getStockFilter().isBlank()) {
            spec = spec.and(hasStockRange(request.getStockFilter(), groupedAttrValues));
        }

        return spec;
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    /** Chỉ lấy sản phẩm đang hoạt động */
    private Specification<Product> isActive() {
        return (root, query, cb) -> cb.isTrue(root.get("isActive"));
    }

    /** Lọc theo danh mục */
//    private Specification<Product> hasCategory(Integer categoryId) {
//        if (categoryId == null) return null;
//        return (root, query, cb) -> cb.equal(root.get("category").get("id"), categoryId);
//    }

    /** Lọc theo thương hiệu */
    private Specification<Product> hasBrand(Integer brandId) {
        if (brandId == null) return null;
        return (root, query, cb) -> cb.equal(root.get("brand").get("id"), brandId);
    }

    /** Lọc theo khoảng giá */
    private Specification<Product> hasPriceBetween(BigDecimal minPrice, BigDecimal maxPrice) {
        if (minPrice == null && maxPrice == null) return null;

        return (root, query, cb) -> {
            if (minPrice != null && maxPrice != null) {
                return cb.between(root.get("basePrice"), minPrice, maxPrice);
            }
            if (minPrice != null) {
                return cb.greaterThanOrEqualTo(root.get("basePrice"), minPrice);
            }
            return cb.lessThanOrEqualTo(root.get("basePrice"), maxPrice);
        };
    }

    /**
     * Lọc theo 1 nhóm attribute với logic OR.
     *
     * Điều kiện: Sản phẩm phải có ÍT NHẤT 1 SKU chứa ÍT NHẤT 1 attributeValue
     * trong danh sách ids của nhóm này.
     *
     * SQL tương đương:
     * EXISTS (
     *   SELECT 1 FROM product_skus sk
     *   JOIN sku_values sv ON sv.sku_id = sk.id
     *   WHERE sk.product_id = p.id
     *     AND sv.attribute_value_id IN (:ids)
     * )
     */
    private Specification<Product> hasAnyAttributeValueInGroup(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) return null;

        return (root, query, cb) -> {
            Subquery<Integer> subquery = query.subquery(Integer.class);
            Root<Product> corrRoot = subquery.correlate(root);

            Join<?, ?> skuJoin      = corrRoot.join("skus");
            Join<?, ?> skuValueJoin = skuJoin.join("skuValues");

            subquery.select(skuJoin.get("id"));
            subquery.where(skuValueJoin.get("attributeValue").get("id").in(ids));

            return cb.exists(subquery);
        };
    }

    private Specification<Product> hasCategoryIn(Set<Integer> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) return null;
        return (root, query, cb) -> root.get("category").get("id").in(categoryIds);
    }

    private Specification<Product> hasStockRange(String rawStockFilter, Map<Integer, List<Integer>> groupedAttrValues) {
        return (root, query, cb) -> {
            Expression<Integer> totalStock = matchingSkuStockSum(root, query, cb, groupedAttrValues);
            String stockFilter = normalizeStockFilter(rawStockFilter);

            return switch (stockFilter) {
                case "OUT_OF_STOCK" -> cb.equal(totalStock, 0);
                case "LOW_STOCK" -> cb.and(
                        cb.greaterThan(totalStock, 0),
                        cb.lessThanOrEqualTo(totalStock, 10)
                );
                case "IN_STOCK" -> cb.greaterThan(totalStock, 10);
                default -> cb.conjunction();
            };
        };
    }

    private Expression<Integer> matchingSkuStockSum(
            Root<Product> root,
            jakarta.persistence.criteria.CriteriaQuery<?> query,
            jakarta.persistence.criteria.CriteriaBuilder cb,
            Map<Integer, List<Integer>> groupedAttrValues
    ) {
        Subquery<Integer> subquery = query.subquery(Integer.class);
        Root<Product> correlatedProduct = subquery.correlate(root);
        Join<?, ?> skuJoin = correlatedProduct.join("skus");

        List<Predicate> predicates = new ArrayList<>();
        predicates.add(cb.isTrue(skuJoin.get("isActive")));

        if (groupedAttrValues != null) {
            for (List<Integer> idsInGroup : groupedAttrValues.values()) {
                if (idsInGroup == null || idsInGroup.isEmpty()) {
                    continue;
                }
                Join<?, ?> skuValueJoin = skuJoin.join("skuValues");
                predicates.add(skuValueJoin.get("attributeValue").get("id").in(idsInGroup));
            }
        }

        subquery.select(cb.coalesce(cb.sum(skuJoin.get("stockQuantity")), 0));
        if (!predicates.isEmpty()) {
            subquery.where(predicates.toArray(new Predicate[0]));
        }
        return subquery;
    }

    private String normalizeStockFilter(String rawStockFilter) {
        String value = rawStockFilter.trim().toUpperCase();
        return switch (value) {
            case "0", "=0", "OUT", "OUT_OF_STOCK", "HET_HANG" -> "OUT_OF_STOCK";
            case "<=10", "LOW", "LOW_STOCK", "SAP_HET_HANG" -> "LOW_STOCK";
            case ">10", "IN", "IN_STOCK", "CON_HANG" -> "IN_STOCK";
            default -> value;
        };
    }
}
