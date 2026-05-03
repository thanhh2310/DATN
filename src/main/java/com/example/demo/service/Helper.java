package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.model.ProductSku;
import com.example.demo.repository.CategoryRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.text.Normalizer;
import java.util.regex.Pattern;

@Component
@RequiredArgsConstructor
public class Helper {
    private final CategoryRepository categoryRepository;
    private final UserRepository userRepository;

    private String generateSlug(String name) {
        if (name == null) return null;

        // 1. Chuyển thành chữ thường
        String temp = name.toLowerCase();

        // 2. Chuẩn hóa Unicode để tách dấu ra khỏi chữ (Ví dụ: ấ -> a + dấu sắc)
        temp = Normalizer.normalize(temp, Normalizer.Form.NFD);

        // 3. Dùng Regex để loại bỏ các dấu
        Pattern pattern = Pattern.compile("\\p{InCombiningDiacriticalMarks}+");
        temp = pattern.matcher(temp).replaceAll("");

        // 4. Thay thế chữ Đ/đ thành d (Vì Normalizer không xử lý chữ Đ)
        temp = temp.replace("đ", "d");

        // 5. Thay khoảng trắng và các ký tự đặc biệt thành dấu gạch ngang
        temp = temp.replaceAll("[^a-z0-9\\s-]", ""); // Bỏ ký tự lạ
        temp = temp.replaceAll("\\s+", "-"); // Khoảng trắng thành -

        return temp;
    }

    public String generateUniqueSlug(String name) {
        if (name == null) return null;

        String baseSlug = generateSlug(name); // slug gốc
        String slug = baseSlug;
        int counter = 1;

        // Loop đến khi tìm được slug chưa tồn tại
        while (categoryRepository.existsBySlug(slug)) {
            slug = baseSlug + "-" + counter;
            counter++;
        }
        return slug;
    }

    public Integer getCurrentUserId() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND))
                .getId();
    }

    public String resolveSkuImage(ProductSku sku) {
        if (sku.getImages() != null && !sku.getImages().isEmpty()) {
            return sku.getImages().stream()
                    .filter(img -> Boolean.TRUE.equals(img.getIsThumbnail()))
                    .findFirst()
                    .orElse(sku.getImages().iterator().next())
                    .getImageUrl();
        }
        if (sku.getProduct().getImages() != null && !sku.getProduct().getImages().isEmpty()) {
            return sku.getProduct().getImages().iterator().next().getImageUrl();
        }
        return null;
    }
}
