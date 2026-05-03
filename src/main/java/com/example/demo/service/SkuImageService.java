package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.SkuImageRequest;
import com.example.demo.dto.response.SkuImageResponse;
import com.example.demo.model.ProductSku;
import com.example.demo.model.ProductSkuImage;
import com.example.demo.repository.ProductSkuImageRepository;
import com.example.demo.repository.ProductSkuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SkuImageService {

    private final ProductSkuRepository productSkuRepository;
    private final ProductSkuImageRepository productSkuImageRepository;

    @Transactional
    public SkuImageResponse addImage(Integer skuId, SkuImageRequest request) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        // Nếu đặt là thumbnail, unset các thumbnail khác
        if (Boolean.TRUE.equals(request.getIsThumbnail())) {
            sku.getImages().forEach(img -> img.setIsThumbnail(false));
        }

        // Tự động tính displayOrder nếu không được cung cấp
        int displayOrder = request.getDisplayOrder() != null ? request.getDisplayOrder() : sku.getImages().size();

        ProductSkuImage image = ProductSkuImage.builder()
                .productSku(sku)
                .imageUrl(request.getImageUrl())
                .isThumbnail(request.getIsThumbnail())
                .displayOrder(displayOrder)
                .build();

        sku.getImages().add(image);
        productSkuImageRepository.flush();

        return SkuImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(image.getIsThumbnail())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    @Transactional
    public void deleteImage(Integer skuId, Integer imageId) {
        ProductSkuImage image = productSkuImageRepository.findById(imageId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.IMAGE_NOT_FOUND));

        if (!image.getProductSku().getId().equals(skuId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // Nếu ảnh đang là thumbnail, set ảnh khác làm thumbnail
        if (Boolean.TRUE.equals(image.getIsThumbnail())) {
            image.getProductSku().getImages().stream()
                    .filter(img -> !img.getId().equals(imageId))
                    .sorted(Comparator.comparingInt(ProductSkuImage::getDisplayOrder))
                    .findFirst()
                    .ifPresent(firstImg -> firstImg.setIsThumbnail(true));
        }

        image.getProductSku().getImages().remove(image);
    }

    @Transactional
    public SkuImageResponse updateImage(Integer skuId, Integer imageId, SkuImageRequest request) {
        ProductSkuImage image = productSkuImageRepository.findById(imageId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.IMAGE_NOT_FOUND));

        if (!image.getProductSku().getId().equals(skuId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        // Nếu đặt là thumbnail, unset các thumbnail khác
        if (Boolean.TRUE.equals(request.getIsThumbnail())) {
            image.getProductSku().getImages().forEach(img -> img.setIsThumbnail(false));
        }

        image.setImageUrl(request.getImageUrl());
        image.setIsThumbnail(request.getIsThumbnail());
        image.setDisplayOrder(request.getDisplayOrder());

        return SkuImageResponse.builder()
                .id(image.getId())
                .imageUrl(image.getImageUrl())
                .isThumbnail(image.getIsThumbnail())
                .displayOrder(image.getDisplayOrder())
                .build();
    }

    public List<SkuImageResponse> getImages(Integer skuId) {
        ProductSku sku = productSkuRepository.findById(skuId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.SKU_NOT_FOUND));

        return sku.getImages().stream()
                .sorted(Comparator.comparingInt(ProductSkuImage::getDisplayOrder))
                .map(img -> SkuImageResponse.builder()
                        .id(img.getId())
                        .imageUrl(img.getImageUrl())
                        .isThumbnail(img.getIsThumbnail())
                        .displayOrder(img.getDisplayOrder())
                        .build())
                .collect(Collectors.toList());
    }
}
