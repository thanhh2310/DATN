package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.ProductCreationRequest;
import com.example.demo.dto.request.ProductSkuRequest;
import com.example.demo.dto.request.ProductSpecRequest;
import com.example.demo.dto.request.ProductUpdateRequest;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final BrandRepository brandRepository;
    private final ProductImageRepository productImageRepository;
    private final ProductSpecRepository productSpecRepository;
    private final ProductSkuRepository productSkuRepository;
    private final AttributeRepository attributeRepository;
    private final AttributeValueRepository attributeValueRepository;
    private final ProductMapper productMapper;

    @Transactional
    public void createProductDetail(ProductCreationRequest request) {

        // 1. Kiểm tra Category và Brand có tồn tại không
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));

        // LẤY DANH SÁCH CÁC THUỘC TÍNH HỢP LỆ CHO DANH MỤC NÀY
        // Giả định trong Entity Category bạn đã có: Set<CategoryAttribute> categoryAttributes;
        Set<Integer> validAttributeIds = category.getCategoryAttributes().stream()
                .map(ca -> ca.getAttribute().getId())
                .collect(Collectors.toSet());

        // 2. Lưu bảng cha: PRODUCT (Bổ sung hàm check trùng Slug nếu cần)
        Product product = Product.builder()
                .name(request.getName())
                .slug(request.getSlug())
                .description(request.getDescription())
                .basePrice(request.getBasePrice())
                .category(category)
                .brand(brand)
                .isActive(true)
                .build();
        product = productRepository.save(product);

        // 3. Lưu danh sách ẢNH SẢN PHẨM (ProductImage)
        if (request.getImageUrls() != null && !request.getImageUrls().isEmpty()) {
            List<ProductImage> images = new ArrayList<>();
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                images.add(ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .displayOrder(i)
                        .build());
            }
            productImageRepository.saveAll(images);
        }

        // 4. Lưu THÔNG SỐ KỸ THUẬT CHUNG (ProductSpec)
        if (request.getSpecs() != null && !request.getSpecs().isEmpty()) {
            // Tối ưu N+1: Gom tất cả ID lại và query 1 lần duy nhất
            Set<Integer> specAttributeIds = request.getSpecs().stream()
                    .map(ProductSpecRequest::getAttributeId)
                    .collect(Collectors.toSet());

            // Validate: Thuộc tính này có được phép dùng cho Category này không?
            for (Integer attrId : specAttributeIds) {
                if (!validAttributeIds.contains(attrId)) {
                    // Bạn cần định nghĩa thêm ErrorCode này
                    throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_ALLOWED_FOR_CATEGORY);
                }
            }

            // Lấy tất cả Attribute từ DB lên trong 1 câu query
            Map<Integer, Attribute> attributeMap = attributeRepository.findAllById(specAttributeIds)
                    .stream().collect(Collectors.toMap(Attribute::getId, attr -> attr));

            List<ProductSpec> specs = new ArrayList<>();
            for (var specReq : request.getSpecs()) {
                Attribute attribute = attributeMap.get(specReq.getAttributeId());
                if (attribute == null) {
                    throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND);
                }

                specs.add(ProductSpec.builder()
                        .product(product)
                        .attribute(attribute)
                        .value(specReq.getValue())
                        .build());
            }
            productSpecRepository.saveAll(specs);
        }

        // 5. Lưu BIẾN THỂ SẢN PHẨM (ProductSku) và ÁNH XẠ THUỘC TÍNH (sku_values)
        if (request.getSkus() != null && !request.getSkus().isEmpty()) {

            // Tối ưu: Lấy danh sách skuCode client gửi lên để check trùng lặp trước
            Set<String> skuCodes = request.getSkus().stream()
                    .map(ProductSkuRequest::getSkuCode)
                    .collect(Collectors.toSet());

            // Kiểm tra xem có mã SKU nào đã tồn tại trong DB chưa (Query 1 lần)
            if (productSkuRepository.existsBySkuCodeIn(skuCodes)) {
                throw new WebErrorConfig(ErrorCode.SKU_CODE_ALREADY_EXISTED);
            }

            List<ProductSku> skusToSave = new ArrayList<>();

            for (var skuReq : request.getSkus()) {
                ProductSku sku = ProductSku.builder()
                        .product(product)
                        .skuCode(skuReq.getSkuCode())
                        .price(skuReq.getPrice())
                        .stockQuantity(skuReq.getStockQuantity())
                        .imageUrl(skuReq.getImageUrl())
                        .isActive(true)
                        .skuValues(new java.util.HashSet<>())
                        .build();

                if (skuReq.getAttributeValueIds() != null && !skuReq.getAttributeValueIds().isEmpty()) {
                    List<AttributeValue> attrValues = attributeValueRepository.findAllById(skuReq.getAttributeValueIds());

                    for (AttributeValue attrValue : attrValues) {
                        // Validate quan trọng: Giá trị thuộc tính này có thuộc về 1 Thuộc tính được phép của Category không?
                        if (!validAttributeIds.contains(attrValue.getAttribute().getId())) {
                            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_ALLOWED_FOR_CATEGORY);
                        }

                        SkuValue skuValue = SkuValue.builder()
                                .productSku(sku)
                                .attributeValue(attrValue)
                                .build();
                        sku.getSkuValues().add(skuValue);
                    }
                }
                skusToSave.add(sku);
            }

            productSkuRepository.saveAll(skusToSave);
        }
    }

    // =========================================================================
    // R - READ (LẤY CHI TIẾT 1 SẢN PHẨM)
    // =========================================================================
    public ProductResponse getProductById(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        // Trả về DTO chứa đầy đủ thông tin: Core, Images, Specs, SKUs
        return productMapper.toProductResponse(product);
    }

    // =========================================================================
    // R - READ (LẤY DANH SÁCH CÓ PHÂN TRANG CHO ADMIN)
    // =========================================================================
    public PageResponse<ProductResponse> getAllProducts(int pageNumber, int pageSize) {
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("createdAt").descending());

        Page<Product> pageData = productRepository.findAll(pageable);

        List<ProductResponse> productResponses = pageData.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .currentPage(pageNumber)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .data(productResponses)
                .build();
    }

    // =========================================================================
    // U - UPDATE (CẬP NHẬT THÔNG TIN SẢN PHẨM)
    // =========================================================================
    @Transactional
    public ProductResponse updateProduct(Integer id, ProductUpdateRequest request) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        // 1. Cập nhật thông tin cơ bản (Core)
        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));
            product.setCategory(category);
        }
        if (request.getBrandId() != null) {
            Brand brand = brandRepository.findById(request.getBrandId())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));
            product.setBrand(brand);
        }

        product.setName(request.getName());
        product.setSlug(request.getSlug());
        product.setDescription(request.getDescription());
        product.setBasePrice(request.getBasePrice());
        product.setIsActive(request.getIsActive());

        // 2. Cập nhật Hình ảnh (Xóa hết ảnh cũ, nạp ảnh mới)
        if (request.getImageUrls() != null) {
            product.getImages().clear(); // Nhờ orphanRemoval=true, DB sẽ tự động DELETE các dòng cũ
            for (int i = 0; i < request.getImageUrls().size(); i++) {
                product.getImages().add(ProductImage.builder()
                        .product(product)
                        .imageUrl(request.getImageUrls().get(i))
                        .isThumbnail(i == 0)
                        .displayOrder(i)
                        .build());
            }
        }

        // 3. Cập nhật Thông số kỹ thuật chung (Xóa hết nạp lại)
        if (request.getSpecs() != null) {
            product.getSpecs().clear();
            for (var specReq : request.getSpecs()) {
                Attribute attribute = attributeRepository.findById(specReq.getAttributeId())
                        .orElseThrow(() -> new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_FOUND));
                product.getSpecs().add(ProductSpec.builder()
                        .product(product)
                        .attribute(attribute)
                        .value(specReq.getValue())
                        .build());
            }
        }

        // LƯU Ý CHO SENIOR:
        // Chúng ta KHÔNG update trực tiếp list SKUs ở đây bằng lệnh clear() giống Images và Specs.
        // Vì nếu xóa 1 SKU đang nằm trong giỏ hàng (CartItemMapper) của khách, hệ thống sẽ sập.
        // Việc thêm/sửa tồn kho, đổi giá SKU nên được tách ra 1 API riêng biệt (VD: PUT /api/skus/{id}).

        product = productRepository.save(product);
        return productMapper.toProductResponse(product);
    }

    // =========================================================================
    // D - DELETE (XÓA MỀM SẢN PHẨM)
    // =========================================================================
    @Transactional
    public void deleteProduct(Integer id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.PRODUCT_NOT_FOUND));

        // Khóa sản phẩm lại để không hiển thị trên Web nữa
        product.setIsActive(false);
        productRepository.save(product);

        // Cập nhật trạng thái cho toàn bộ các phiên bản (SKU) của sản phẩm này
        // Tránh việc khách hàng vô tình có đường link SKU cũ vẫn đặt hàng được
        product.getSkus().forEach(sku -> sku.setIsActive(false));

        // (Tùy chọn) Gửi event xóa sản phẩm ra khỏi Cache Redis hoặc Elasticsearch ở đây
    }
}