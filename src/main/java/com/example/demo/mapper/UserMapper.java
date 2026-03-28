package com.example.demo.mapper;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.Enum.RoleName;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.RegisterRequest;
import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.request.UserCreationRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
public class UserMapper {
    private final PasswordEncoder passwordEncoder;
    private final RoleRepository roleRepository;

    public User registerToUser (RegisterRequest request){
        Role role = roleRepository.findByName(RoleName.ROLE_USER.name())
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));

        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(false)
                .userRoles(new HashSet<>())
                .build();

        UserRole userRole = UserRole.builder()
                .user(user)
                .role(role)
                .build();
        user.getUserRoles().add(userRole);

        return user;
    }

    public ProfileResponse userToProfileResponse(User user){
        return ProfileResponse.builder()
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .build();
    }

    public void updateUserFromProfileRequest(User user, UpdateProfileRequest request){
        if(request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if(request.getLastName() != null) user.setLastName(request.getLastName());
        if(request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());
        // Lưu ý: Thường không cho user tự đổi email ở profile đơn giản vì liên quan đến login
    }

    public UserResponse fromUser(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(user.getFirstName())
                .lastName(user.getLastName())
                .phoneNumber(user.getPhoneNumber())
                .isActive(user.getIsActive())
                .createdAt(user.getCreatedAt())
                // SỬA Ở ĐÂY: Trích xuất tên Role từ UserRole
                .roles(user.getUserRoles().stream()
                        .map(userRole -> userRole.getRole().getName())
                        .collect(Collectors.toSet()))
                .build();
    }

    public User userCreationToUser(UserCreationRequest request){
        User user = User.builder()
                .email(request.getEmail())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .firstName(request.getFirstName())
                .lastName(request.getLastName())
                .phoneNumber(request.getPhoneNumber())
                .isActive(request.getIsActive())
                .userRoles(new HashSet<>())
                .build();

        // Lấy danh sách Role và chuyển thành UserRole
        Set<UserRole> userRoles = request.getRoles().stream()
                .map(roleName -> {
                    Role role = roleRepository.findByName(roleName)
                            .orElseThrow(()-> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));
                    return UserRole.builder().user(user).role(role).build();
                })
                .collect(Collectors.toSet());

        user.setUserRoles(userRoles);
        return user;
    }

    public void updateUserFromAdminRequest(User user, UserUpdateRequest request){
        if(request.getFirstName() != null) user.setFirstName(request.getFirstName());
        if(request.getLastName() != null) user.setLastName(request.getLastName());
        if(request.getPhoneNumber() != null) user.setPhoneNumber(request.getPhoneNumber());

        // Update Role một cách an toàn cho bảng trung gian
        if(request.getRoles() != null && !request.getRoles().isEmpty()){
            // Xóa hết quan hệ cũ (Hibernate sẽ tự động delete dưới DB nhờ orphanRemoval=true)
            user.getUserRoles().clear();

            // Tạo danh sách quan hệ mới
            Set<UserRole> newRoles = request.getRoles().stream()
                    .map(roleName -> {
                        Role role = roleRepository.findByName(roleName)
                                .orElseThrow(()-> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));
                        return UserRole.builder().user(user).role(role).build();
                    })
                    .collect(Collectors.toSet());

            // Add mới vào
            user.getUserRoles().addAll(newRoles);
        }
    }
}
