package com.example.demo.repository;

import com.example.demo.model.AttributeValue;
import com.example.demo.model.Product;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeValueRepository extends JpaRepository<AttributeValue, Integer> {
}
