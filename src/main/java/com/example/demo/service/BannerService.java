package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.BannerRequest;
import com.example.demo.dto.response.BannerResponse;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.mapper.BannerMapper;
import com.example.demo.model.Banner;
import com.example.demo.repository.BannerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class BannerService {
    private static final String DEFAULT_POSITION = "HOME_MAIN";

    private final BannerRepository bannerRepository;
    private final BannerMapper bannerMapper;

    @Transactional
    public BannerResponse create(BannerRequest request) {
        Banner banner = Banner.builder()
                .title(request.getTitle())
                .imageUrl(request.getImageUrl())
                .targetUrl(request.getTargetUrl())
                .position(normalizePosition(request.getPosition()))
                .displayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0)
                .isActive(request.getIsActive() != null ? request.getIsActive() : true)
                .build();

        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse update(Integer id, BannerRequest request) {
        Banner banner = getBanner(id);

        banner.setTitle(request.getTitle());
        banner.setImageUrl(request.getImageUrl());
        banner.setTargetUrl(request.getTargetUrl());
        banner.setPosition(normalizePosition(request.getPosition()));
        banner.setDisplayOrder(request.getDisplayOrder() != null ? request.getDisplayOrder() : 0);
        banner.setIsActive(request.getIsActive() != null ? request.getIsActive() : true);

        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public BannerResponse toggleStatus(Integer id) {
        Banner banner = getBanner(id);
        banner.setIsActive(!Boolean.TRUE.equals(banner.getIsActive()));
        return bannerMapper.toResponse(bannerRepository.save(banner));
    }

    @Transactional
    public void delete(Integer id) {
        Banner banner = getBanner(id);
        bannerRepository.delete(banner);
    }

    @Transactional(readOnly = true)
    public BannerResponse getById(Integer id) {
        return bannerMapper.toResponse(getBanner(id));
    }

    @Transactional(readOnly = true)
    public PageResponse<BannerResponse> getAll(String position, int page, int size) {
        int pageNumber = page > 0 ? page - 1 : 0;
        Pageable pageable = PageRequest.of(
                pageNumber,
                size,
                Sort.by(Sort.Order.asc("position"), Sort.Order.asc("displayOrder"), Sort.Order.desc("createdAt"))
        );

        Page<Banner> bannerPage = position == null || position.isBlank()
                ? bannerRepository.findAll(pageable)
                : bannerRepository.findByPositionIgnoreCase(position.trim(), pageable);

        return PageResponse.<BannerResponse>builder()
                .currentPage(page)
                .totalPage(bannerPage.getTotalPages())
                .pageSize(bannerPage.getSize())
                .totalElements(bannerPage.getTotalElements())
                .items(bannerPage.getContent().stream().map(bannerMapper::toResponse).toList())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BannerResponse> getActive(String position) {
        List<Banner> banners = position == null || position.isBlank()
                ? bannerRepository.findByIsActiveTrueOrderByPositionAscDisplayOrderAscCreatedAtDesc()
                : bannerRepository.findByPositionIgnoreCaseAndIsActiveTrueOrderByDisplayOrderAscCreatedAtDesc(position.trim());

        return banners.stream().map(bannerMapper::toResponse).toList();
    }

    private Banner getBanner(Integer id) {
        return bannerRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BANNER_NOT_FOUND));
    }

    private String normalizePosition(String position) {
        if (position == null || position.isBlank()) {
            return DEFAULT_POSITION;
        }
        return position.trim().toUpperCase();
    }
}
