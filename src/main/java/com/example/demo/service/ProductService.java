package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.ProductCreationRequest;
import com.example.demo.dto.request.ProductFilterRequest;
import com.example.demo.dto.request.ProductSkuRequest;
import com.example.demo.dto.request.ProductSpecRequest;
import com.example.demo.dto.request.ProductUpdateRequest;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProductResponse;
import com.example.demo.mapper.ProductMapper;
import com.example.demo.model.*;
import com.example.demo.repository.*;
import com.example.demo.util.UniqueTextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    private final AttributeValueRepository attributeValueRepository;
    private final ProductMapper productMapper;

    @Transactional
    public void createProductDetail(ProductCreationRequest request) {
        validateUniqueProductName(request.getName(), null);
        validateUniqueProductSlug(request.getSlug(), null);

        // 1. Kiểm tra Category và Brand có tồn tại không
        Category category = categoryRepository.findById(request.getCategoryId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.CATEGORY_NOT_FOUND));

        Brand brand = brandRepository.findById(request.getBrandId())
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.BRAND_NOT_FOUND));

        // Bắt lỗi duplicate để tránh ID tăng rác trong DB
        if (productRepository.existsBySlug(request.getSlug())) {
            throw new WebErrorConfig(ErrorCode.SLUG_ALREADY_EXISTED);
        }

        // LẤY DANH SÁCH CÁC THUỘC TÍNH HỢP LỆ CHO DANH MỤC NÀY
        // Giả định trong Entity Category bạn đã có: Set<CategoryAttribute>
        // categoryAttributes;
        Set<Integer> validAttributeIds = category.getCategoryAttributes().stream()
                .map(ca -> ca.getAttribute().getId())
                .collect(Collectors.toSet());

        // Validate: Không có attributeValueId trùng lặp trong danh sách specs
        if (request.getSpecs() != null && !request.getSpecs().isEmpty()) {
            validateUniqueSpecAttributeValueIds(request.getSpecs());
        }

        validateUniqueSkuAttributeValues(request.getSkus());

        // 2. Lưu bảng cha: PRODUCT (Bổ sung hàm check trùng Slug nếu cần)
        Product product = Product.builder()
                .name(request.getName().trim())
                .slug(request.getSlug().trim())
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
            // Tối ưu N+1: Gom tất cả attributeValueId lại và query 1 lần duy nhất
            Set<Integer> specAttributeValueIds = request.getSpecs().stream()
                    .map(ProductSpecRequest::getAttributeValueId)
                    .collect(Collectors.toSet());

            // Lấy tất cả AttributeValue từ DB lên trong 1 câu query
            Map<Integer, AttributeValue> attributeValueMap = attributeValueRepository.findAllById(specAttributeValueIds)
                    .stream().collect(Collectors.toMap(AttributeValue::getId, av -> av));

            List<ProductSpec> specs = new ArrayList<>();
            for (var specReq : request.getSpecs()) {
                AttributeValue attributeValue = attributeValueMap.get(specReq.getAttributeValueId());
                if (attributeValue == null) {
                    throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
                }

                // Validate: Thuộc tính này có được phép dùng cho Category này không?
                if (!validAttributeIds.contains(attributeValue.getAttribute().getId())) {
                    throw new WebErrorConfig(ErrorCode.ATTRIBUTE_NOT_ALLOWED_FOR_CATEGORY);
                }
                if (!attributeValue.getAttribute().getId().equals(specReq.getAttributeId())) {
                    throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                }

                specs.add(ProductSpec.builder()
                        .product(product)
                        .attributeValue(attributeValue)
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
                        .isActive(true)
                        .skuValues(new java.util.HashSet<>())
                        .images(new java.util.HashSet<>())
                        .build();

                if (skuReq.getImageUrls() != null && !skuReq.getImageUrls().isEmpty()) {
                    for (int i = 0; i < skuReq.getImageUrls().size(); i++) {
                        sku.getImages().add(ProductSkuImage.builder()
                                .productSku(sku)
                                .imageUrl(skuReq.getImageUrls().get(i))
                                .isThumbnail(i == 0)
                                .displayOrder(i)
                                .build());
                    }
                }

                if (skuReq.getAttributeValueIds() != null && !skuReq.getAttributeValueIds().isEmpty()) {
                    validateUniqueIntegerIds(skuReq.getAttributeValueIds());
                    List<AttributeValue> attrValues = attributeValueRepository
                            .findAllById(skuReq.getAttributeValueIds());
                    validateAllAttributeValuesExist(skuReq.getAttributeValueIds(), attrValues);
                    validateOneValuePerAttribute(attrValues);

                    for (AttributeValue attrValue : attrValues) {
                        // Validate quan trọng: Giá trị thuộc tính này có thuộc về 1 Thuộc tính được
                        // phép của Category không?
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

//        validateUniqueProductName(request.getName(), id);
//        validateUniqueProductSlug(request.getSlug(), id);

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
                .items(productResponses)
                .build();
    }

    public PageResponse<ProductResponse> getAllProductIsActive(int pageNumber, int pageSize){
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("createdAt").descending());

        Page<Product> pageData = productRepository.findAllByIsActiveTrue(pageable);

        List<ProductResponse> productResponses = pageData.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .currentPage(pageNumber)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .items(productResponses)
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

        product.setName(request.getName().trim());
        product.setSlug(request.getSlug().trim());
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

        // 3. Cập nhật Thông số kỹ thuật chung (Smart Update chuẩn JPA)
        if (request.getSpecs() != null) {
            validateUniqueSpecAttributeValueIds(request.getSpecs());

            // Bước A: Gom danh sách ID các thông số mà Frontend gửi lên
            Set<Integer> newAttrValueIds = request.getSpecs().stream()
                    .map(ProductSpecRequest::getAttributeValueId)
                    .collect(Collectors.toSet());

            // Bước B: XÓA các thông số cũ trong RAM nếu Frontend không gửi lên nữa
            // Hibernate sẽ tự động hiểu và xếp lịch DELETE dưới DB
            product.getSpecs().removeIf(spec -> !newAttrValueIds.contains(spec.getAttributeValue().getId()));

            // Bước C: Tìm các thông số ĐÃ TỒN TẠI để KHÔNG ADD LẠI (Triệt tiêu 100% lỗi Duplicate Key)
            Set<Integer> existingAttrValueIds = product.getSpecs().stream()
                    .map(spec -> spec.getAttributeValue().getId())
                    .collect(Collectors.toSet());

            // Load hàng loạt AttributeValue từ DB lên để chuẩn bị gán
            Map<Integer, AttributeValue> attributeValueMap = attributeValueRepository.findAllById(newAttrValueIds)
                    .stream().collect(Collectors.toMap(AttributeValue::getId, av -> av));

            // Bước D: THÊM MỚI những thông số chưa từng có
            for (var specReq : request.getSpecs()) {
                if (!existingAttrValueIds.contains(specReq.getAttributeValueId())) {
                    AttributeValue attributeValue = attributeValueMap.get(specReq.getAttributeValueId());
                    if (attributeValue == null) {
                        throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
                    }
                    if (!attributeValue.getAttribute().getId().equals(specReq.getAttributeId())) {
                        throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
                    }

                    // CHÚ Ý: Add thẳng vào List của Product, tuyệt đối không dùng productSpecRepository.save()
                    product.getSpecs().add(ProductSpec.builder()
                            .product(product)
                            .attributeValue(attributeValue)
                            .build());
                }
            }
        }

        // KHÔNG update trực tiếp list SKUs ở đây bằng lệnh clear() giống Images và
        // Specs.
        // Vì nếu xóa 1 SKU đang nằm trong giỏ hàng (CartItemMapper) của khách, hệ thống
        // sẽ sập.
        // Việc thêm/sửa tồn kho, đổi giá SKU nên được tách ra 1 API riêng biệt (VD: PUT
        // /api/skus/{id}).

        // 4. Cập nhật ảnh cho từng SKU (Nếu có request)

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

        // (Tùy chọn) Gửi event xóa sản phẩm ra khỏi Cache Redis hoặc Elasticsearch ở
        // đây
    }

    // =========================================================================
    // FILTER - LỌC SẢN PHẨM THEO TIÊU CHÍ
    // =========================================================================
    public PageResponse<ProductResponse> filterProducts(ProductFilterRequest request) {
        // 1. Build sort từ request.sortBy
        Sort sort = buildSort(request.getSortBy());

        // 2. Tạo Pageable
        Pageable pageable = PageRequest.of(request.getPageNumber() - 1, request.getPageSize(), sort);
        // Xử lý Category: Thu thập tất cả ID con nếu truyền vào category cha
        Set<Integer> categoryIds = null;
        if (request.getCategoryId() != null) {
            categoryIds = collectAllCategoryIds(request.getCategoryId());
        }

        // 3. Xử lý gom nhóm AttributeValues trước khi ném vào Specification
        Map<Integer, List<Integer>> groupedAttrValues = null;
        if (request.getAttributeValueIds() != null && !request.getAttributeValueIds().isEmpty()) {
            // Query DB 1 lần để lấy toàn bộ AttributeValue khách chọn
            List<AttributeValue> attrValues = attributeValueRepository.findAllById(request.getAttributeValueIds());

            // Dùng Stream API để gom nhóm theo Attribute ID (Ví dụ: 1 -> [Đỏ, Xanh], 2 ->
            // [Size 42])
            groupedAttrValues = attrValues.stream()
                    .collect(Collectors.groupingBy(
                            av -> av.getAttribute().getId(), // Key là ID của nhóm thuộc tính
                            Collectors.mapping(AttributeValue::getId, Collectors.toList()) // Value là mảng ID các giá
                                                                                           // trị
                    ));
        }

        // 4. Build Specification động (Truyền thêm groupedAttrValues)
        Specification<Product> spec = ProductSpecification.buildFilter(request, groupedAttrValues, categoryIds);

        // 5. Query
        Page<Product> pageData = productRepository.findAll(spec, pageable);

        // 6. Map sang response
        List<ProductResponse> responses = pageData.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .currentPage(request.getPageNumber())
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .items(responses)
                .build();
    }

    // =========================================================================
    // SEARCH - TÌM KIẾM SẢN PHẨM THEO TỪ KHÓA
    // =========================================================================
    public PageResponse<ProductResponse> searchProducts(String keyword, int pageNumber, int pageSize) {
        String safeKeyword = (keyword == null || keyword.trim().isEmpty()) ? "" : keyword.trim();

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize, Sort.by("createdAt").descending());

        Page<Product> pageData = productRepository.searchByKeyword(safeKeyword, pageable);

        List<ProductResponse> responses = pageData.getContent().stream()
                .map(productMapper::toProductResponse)
                .toList();

        return PageResponse.<ProductResponse>builder()
                .currentPage(pageNumber)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .items(responses)
                .build();
    }

    // Helper: Chuyển đổi sortBy string sang Sort object
    private Sort buildSort(String sortBy) {
        if (sortBy == null)
            return Sort.by("createdAt").descending();
        return switch (sortBy) {
            case "price_asc" -> Sort.by("basePrice").ascending();
            case "price_desc" -> Sort.by("basePrice").descending();
            case "name_asc" -> Sort.by("name").ascending();
            case "name_desc" -> Sort.by("name").descending();
            case "newest" -> Sort.by("createdAt").descending();
            case "oldest" -> Sort.by("createdAt").ascending();
            default -> Sort.by("createdAt").descending();
        };
    }

    /**
     * Thu thập tất cả categoryId: bao gồm chính nó + toàn bộ con/cháu đệ quy.
     * VD: Input id=1 ("Điện thoại") → Output: {1, 2, 3}
     */
    private Set<Integer> collectAllCategoryIds(Integer categoryId) {
        Set<Integer> result = new HashSet<>();
        result.add(categoryId); // Thêm chính nó

        // Lấy các danh mục con trực tiếp
        List<Category> children = categoryRepository.findByParentId(categoryId);

        for (Category child : children) {
            result.addAll(collectAllCategoryIds(child.getId())); // Đệ quy xuống cháu
        }

        return result;
    }

    private void validateUniqueSpecAttributeValueIds(List<ProductSpecRequest> specs) {
        validateUniqueIntegerIds(specs.stream()
                .map(ProductSpecRequest::getAttributeValueId)
                .toList());
    }

    private void validateUniqueSkuAttributeValues(List<ProductSkuRequest> skus) {
        if (skus == null) {
            return;
        }

        Set<String> skuCodes = new HashSet<>();
        for (ProductSkuRequest sku : skus) {
            if (sku.getSkuCode() != null && !skuCodes.add(sku.getSkuCode().trim().toLowerCase())) {
                throw new WebErrorConfig(ErrorCode.SKU_CODE_ALREADY_EXISTED);
            }
            if (sku.getAttributeValueIds() != null) {
                validateUniqueIntegerIds(sku.getAttributeValueIds());
            }
        }
    }

    private void validateUniqueIntegerIds(List<Integer> ids) {
        if (ids == null) {
            return;
        }

        if (ids.stream().anyMatch(Objects::isNull)) {
            throw new WebErrorConfig(ErrorCode.INVALID_ATTRIBUTE_VALUE);
        }

        if (new HashSet<>(ids).size() != ids.size()) {
            throw new WebErrorConfig(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE_ID_IN_REQUEST);
        }
    }

    private void validateAllAttributeValuesExist(List<Integer> requestedIds, List<AttributeValue> attrValues) {
        if (attrValues.size() != new HashSet<>(requestedIds).size()) {
            throw new WebErrorConfig(ErrorCode.ATTRIBUTE_VALUE_NOT_FOUND);
        }
    }

    private void validateOneValuePerAttribute(List<AttributeValue> attrValues) {
        Set<Integer> attributeIds = new HashSet<>();
        for (AttributeValue attrValue : attrValues) {
            if (!attributeIds.add(attrValue.getAttribute().getId())) {
                throw new WebErrorConfig(ErrorCode.DUPLICATE_ATTRIBUTE_VALUE_IN_SPECS);
            }
        }
    }

    private void validateUniqueProductName(String name, Integer excludedId) {
        String normalizedName = UniqueTextNormalizer.normalizeForUnique(name);
        boolean existed = productRepository.findAll().stream()
                .filter(product -> excludedId == null || !product.getId().equals(excludedId))
                .anyMatch(product -> UniqueTextNormalizer.normalizeForUnique(product.getName()).equals(normalizedName));

        if (existed) {
            throw new WebErrorConfig(ErrorCode.PRODUCT_ALREADY_EXISTED);
        }
    }

    private void validateUniqueProductSlug(String slug, Integer excludedId) {
        String normalizedSlug = UniqueTextNormalizer.normalizeForUnique(slug);
        boolean existed = productRepository.findAll().stream()
                .filter(product -> excludedId == null || !product.getId().equals(excludedId))
                .anyMatch(product -> UniqueTextNormalizer.normalizeForUnique(product.getSlug()).equals(normalizedSlug));

        if (existed) {
            throw new WebErrorConfig(ErrorCode.SLUG_ALREADY_EXISTED);
        }
    }


}
