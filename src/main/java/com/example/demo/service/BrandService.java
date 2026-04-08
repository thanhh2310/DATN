package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.BrandRequest;
import com.example.demo.dto.response.BrandResponse;
import com.example.demo.mapper.BrandMapper;
import com.example.demo.model.Brand;
import com.example.demo.repository.BrandRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BrandService {
    private final BrandRepository brandRepository;
    private final BrandMapper brandMapper;
    private final Helper helper;

    @Transactional
    public BrandResponse createBrand(BrandRequest request) {
        String slug = helper.generateUniqueSlug(request.getName());
        Brand brand = brandMapper.toBrand(request);
        brand.setSlug(slug);

        try {
            brand = brandRepository.save(brand);
        } catch (DataIntegrityViolationException e) {
            throw new WebErrorConfig(ErrorCode.BRAND_ALREADY_EXISTED);
        }

        return brandMapper.toBrandResponse(brand);
    }

    @Transactional(readOnly = true)
    public List<BrandResponse> getAllBrands() {

        List<Brand> brands = brandRepository.findByIsActiveTrue(
                Sort.by("createdAt").descending()
        );

        return brands.stream()
                .map(brandMapper::toBrandResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public BrandResponse getBrandById(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));
        return brandMapper.toBrandResponse(brand);
    }

    @Transactional
    public BrandResponse updateBrand(Integer id, BrandRequest request) {

        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));

        String newSlug = helper.generateUniqueSlug(request.getName());

        // Check slug trùng (trừ chính nó)
        if (!brand.getSlug().equals(newSlug)
                && brandRepository.existsBySlug(newSlug)) {
            throw new WebErrorConfig(ErrorCode.BRAND_ALREADY_EXISTED);
        }

        brandMapper.updateBrandFromRequest(brand, request);
        brand.setSlug(newSlug);

        try {
            brand = brandRepository.save(brand);
        } catch (DataIntegrityViolationException e) {
            throw new WebErrorConfig(ErrorCode.BRAND_ALREADY_EXISTED);
        }

        return brandMapper.toBrandResponse(brand);
    }


    @Transactional
    public void deleteBrand(Integer id) {
        Brand brand = brandRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));
        brand.setIsActive(false);
        brandRepository.save(brand);
    }
}