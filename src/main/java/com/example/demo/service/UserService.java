package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.Enum.RoleName;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.dto.request.UpdateProfileRequest;
import com.example.demo.dto.request.UserCreationRequest;
import com.example.demo.dto.request.UserUpdateRequest;
import com.example.demo.dto.response.PageResponse;
import com.example.demo.dto.response.ProfileResponse;
import com.example.demo.dto.response.UserResponse;
import com.example.demo.mapper.UserMapper;
import com.example.demo.model.Cart;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.CartRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService implements UserDetailsService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final CartRepository cartRepository;
    private final UserMapper userMapper;
    private final RedisService redisService;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with email: " + email));
    }

    //------- api cho user
    public ProfileResponse getMyProfile(){
        // lay email tu nguoi dung dang đăng nhập
        var authenticate = SecurityContextHolder.getContext().getAuthentication();
        String email = authenticate.getName();

        // tim nguoi dung bang email
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        return userMapper.userToProfileResponse(user);
    }

    @Transactional
    public UserResponse updateProfile(UpdateProfileRequest request){
        // lay email tu nguoi dung dang đăng nhập
        var authenticate = SecurityContextHolder.getContext().getAuthentication();
        String email = authenticate.getName();

        System.out.println("EMAIL: " + email);

        // tim nguoi dung bang email
        User user = userRepository.findByEmail(email)
                .orElseThrow(()-> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUserFromProfileRequest(user,request);
        userRepository.save(user);

        return userMapper.fromUser(user);
    }

    //----- api cho admin
    public PageResponse<UserResponse> getAllUser(int pageNumber, int pageSize ){
        // Tạo Pageable
        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize,
                Sort.by("createdAt").descending());

        // Query DB (Tự động tính limit/offset)
        Page<User> pageData = userRepository.findAll(pageable);

        // Map từ Entity sang Response DTO
        List<UserResponse> userResponses = pageData.getContent().stream()
                .map(userMapper::fromUser)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .currentPage(pageNumber)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .items(userResponses)
                .build();
    }

    public PageResponse<UserResponse> getUsersByRole(String roleName, int pageNumber, int pageSize) {
        String normalizedRoleName = normalizeRoleName(roleName);
        roleRepository.findByName(normalizedRoleName)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));

        Pageable pageable = PageRequest.of(pageNumber - 1, pageSize,
                Sort.by("createdAt").descending());
        Page<User> pageData = userRepository.findByRoleName(normalizedRoleName, pageable);

        List<UserResponse> userResponses = pageData.getContent().stream()
                .map(userMapper::fromUser)
                .collect(Collectors.toList());

        return PageResponse.<UserResponse>builder()
                .currentPage(pageNumber)
                .pageSize(pageData.getSize())
                .totalElements(pageData.getTotalElements())
                .totalPage(pageData.getTotalPages())
                .items(userResponses)
                .build();
    }

    @Transactional
    public UserResponse createUser(UserCreationRequest request){
        if (userRepository.findByEmail(request.getEmail()).isPresent()){
            throw new WebErrorConfig(ErrorCode.USER_AlREADY_EXISTED);
        }
        User user = userMapper.userCreationToUser(request);
        userRepository.save(user);
        return userMapper.fromUser(user);
    }

    @Transactional
    public UserResponse updateUser(UserUpdateRequest request, Integer id){
        User user = userRepository.findById(id)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));
        userMapper.updateUserFromAdminRequest(user, request);
        userRepository.save(user);
        return userMapper.fromUser(user);
    }

    @Transactional
    public void toggleUserStatus(Integer userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new WebErrorConfig(ErrorCode.USER_NOT_FOUND));

        // Kiểm tra trạng thái hiện tại
        if (user.getIsActive() && user.getDeletedAt() == null) {
            userRepository.delete(user);
        } else {
            user.setIsActive(true);
            user.setDeletedAt(null);
            userRepository.save(user);
        }

        redisService.deleteAllRefreshTokensOfUser(user.getEmail());
    }

    @Transactional
    public User processOAuth2PostLogin(String email, String firstName, String lastName) {
        return userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));

            User newUser = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .isActive(true)
                    .passwordHash("")
                    .userRoles(new HashSet<>())
                    .build();

            UserRole mapping = UserRole.builder()
                    .user(newUser)
                    .role(userRole)
                    .build();

            newUser.getUserRoles().add(mapping);

            User savedUser = userRepository.save(newUser);

            // Tạo Cart
            cartRepository.save(Cart.builder().user(savedUser).build());

            return savedUser;
        });
    }

    private String normalizeRoleName(String roleName) {
        if (roleName == null || roleName.isBlank()) {
            throw new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND);
        }
        String cleanRoleName = roleName.trim().toUpperCase();
        return cleanRoleName.startsWith("ROLE_") ? cleanRoleName : "ROLE_" + cleanRoleName;
    }
}
