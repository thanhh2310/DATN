package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.UserAddressRequest;
import com.example.demo.dto.response.UserAddressResponse;
import com.example.demo.mapper.UserAddressMapper;
import com.example.demo.model.User;
import com.example.demo.model.UserAddress;
import com.example.demo.repository.UserAddressRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserAddressService {
    private final UserAddressRepository userAddressRepository;
    private final UserAddressMapper userAddressMapper;
    private final UserRepository userRepository;

    // ================= CREATE =================
    @Transactional
    public UserAddressResponse create(Integer currentUserId, UserAddressRequest request) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        // Logic Nghiệp vụ: Check xem user đã có địa chỉ nào chưa
        long addressCount = userAddressRepository.countByUserId(currentUserId);
        boolean isFirstAddress = (addressCount == 0);

        // Nếu là địa chỉ đầu tiên -> Ép thành Default. Nếu không, theo cờ request gửi lên.
        boolean isDefault = isFirstAddress || Boolean.TRUE.equals(request.getIsDefault());

        if (isDefault && !isFirstAddress) {
            userAddressRepository.clearDefaultByUserId(currentUserId);
        }

        UserAddress address = UserAddress.builder()
                .user(user)
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .city(request.getCity())
                .state(request.getState())
                .country(request.getCountry())
                .postalCode(request.getPostalCode())
                .isDefault(isDefault)
                .build();

        userAddressRepository.save(address);

        return userAddressMapper.toResponse(address);
    }

    // ================= UPDATE =================
    @Transactional
    public UserAddressResponse update(Integer currentUserId, Integer addressId, UserAddressRequest request) {

        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ADDRESS_NOT_FOUND));

        // BẢO MẬT: Kiểm tra địa chỉ này có đúng là của User đang đăng nhập không
        if (!address.getUser().getId().equals(currentUserId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION); // Thêm lỗi này vào Enum nhé
        }

        // Logic cập nhật Default
        if (request.getIsDefault() != null) {
            if (request.getIsDefault()) {
                userAddressRepository.clearDefaultByUserId(currentUserId);
                address.setIsDefault(true);
            } else {
                address.setIsDefault(false);
            }
        }

        if (request.getAddressLine1() != null) address.setAddressLine1(request.getAddressLine1());
        if (request.getAddressLine2() != null) address.setAddressLine2(request.getAddressLine2());
        if (request.getCity() != null) address.setCity(request.getCity());
        if (request.getState() != null) address.setState(request.getState());
        if (request.getCountry() != null) address.setCountry(request.getCountry());
        if (request.getPostalCode() != null) address.setPostalCode(request.getPostalCode());

        userAddressRepository.save(address);

        return userAddressMapper.toResponse(address);
    }

    // ================= DELETE =================
    @Transactional
    public void delete(Integer currentUserId, Integer addressId) {
        UserAddress address = userAddressRepository.findById(addressId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ADDRESS_NOT_FOUND));

        // BẢO MẬT: Kiểm tra quyền sở hữu
        if (!address.getUser().getId().equals(currentUserId)) {
            throw new WebErrorConfig(ErrorCode.UNAUTHORIZED_ACTION);
        }

        boolean wasDefault = address.getIsDefault();
        userAddressRepository.delete(address);

        // Logic Nghiệp vụ: Nếu vừa xóa địa chỉ mặc định, tự động gán cái khác lên làm mặc định
        if (wasDefault) {
            List<UserAddress> remainingAddresses = userAddressRepository.findByUserId(currentUserId);
            if (!remainingAddresses.isEmpty()) {
                UserAddress newDefault = remainingAddresses.get(0); // Lấy cái cũ nhất/hoặc mới nhất
                newDefault.setIsDefault(true);
                userAddressRepository.save(newDefault);
            }
        }
    }

    // ================= GET BY USER =================
    @Transactional(readOnly = true)
    public List<UserAddressResponse> getByUser(Integer currentUserId) {
        // Đã sửa lại cú pháp Stream chuẩn chỉnh
        return userAddressRepository.findByUserId(currentUserId)
                .stream()
                .map(userAddressMapper::toResponse)
                .toList(); //.collect(Collectors.toList());
    }

    // ================= GET DEFAULT =================
    @Transactional(readOnly = true)
    public UserAddressResponse getDefault(Integer currentUserId) {
        UserAddress address = userAddressRepository.findDefaultByUserId(currentUserId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ADDRESS_NOT_FOUND));

        return userAddressMapper.toResponse(address);
    }
}