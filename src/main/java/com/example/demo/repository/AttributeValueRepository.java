package com.example.demo.repository;

import com.example.demo.model.AttributeValue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {
    boolean existsByAttributeIdAndValueIgnoreCase(Integer attributeId, String value);

    boolean existsByAttributeIdAndValueIgnoreCaseAndIdNot(Integer attributeId, String value, Integer id);

    @Query("""
        SELECT COUNT(ps) > 0
        FROM ProductSpec ps
        WHERE ps.attributeValue.id = :attributeValueId
    """)
    boolean isUsedInProductSpecs(@Param("attributeValueId") Integer attributeValueId);

    @Query("""
        SELECT COUNT(sv) > 0
        FROM SkuValue sv
        WHERE sv.attributeValue.id = :attributeValueId
    """)
    boolean isUsedInSkuValues(@Param("attributeValueId") Integer attributeValueId);

    @Query("""
        SELECT COUNT(ps) > 0
        FROM ProductSpec ps
        WHERE ps.attributeValue.attribute.id = :attributeId
    """)
    boolean isAttributeUsedInProductSpecs(@Param("attributeId") Integer attributeId);

    @Query("""
        SELECT COUNT(sv) > 0
        FROM SkuValue sv
        WHERE sv.attributeValue.attribute.id = :attributeId
    """)
    boolean isAttributeUsedInSkuValues(@Param("attributeId") Integer attributeId);
}
