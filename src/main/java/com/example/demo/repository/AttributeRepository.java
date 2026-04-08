package com.example.demo.repository;

import com.example.demo.model.Attribute;
import com.example.demo.model.Product;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AttributeRepository extends JpaRepository<Attribute, Integer> {
    boolean existsByName(@NotBlank(message = "Tên thuộc tính không được để trống") @Size(max = 100, message = "Tên thuộc tính không được vượt quá 100 ký tự") String name);
}
