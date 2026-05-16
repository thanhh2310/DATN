package com.example.demo.config;

import com.example.demo.Enum.RoleName;
import com.example.demo.model.PaymentMethod;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.PaymentMethodRepository;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
public class InitConfig {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordEncoder passwordEncoder;

    @PostConstruct
    @Transactional
    public void init() {
        initRoles();
        initPaymentMethods();
        initAdminAccount();
    }

    private void initRoles() {
        // Lấy tất cả role đã có trong DB
        List<Role> existingRoles = roleRepository.findAll();

        // Convert sang Set để dễ kiểm tra
        Set<String> existingRoleNames = existingRoles.stream()
                .map(Role::getName)
                .collect(Collectors.toSet());

        // Duyệt qua tất cả enum RoleName
        List<Role> rolesToCreate = List.of(RoleName.values()).stream()
                .map(roleEnum -> roleEnum.name())
                .filter(roleName -> !existingRoleNames.contains(roleName))
                .map(roleName -> Role.builder()
                        .name(roleName)
                        .description("Vai trò " + roleName)
                        .build())
                .toList();

        // Lưu các role chưa tồn tại
        if (!rolesToCreate.isEmpty()) {
            roleRepository.saveAll(rolesToCreate);
            log.info("INIT: Created roles {}", rolesToCreate);
        } else {
            log.info("INIT: All roles already exist");
        }
    }

    private void initAdminAccount() {
        if (userRepository.findByEmail("admin@admin.com").isEmpty()) {
            Role adminRole = roleRepository.findByName(RoleName.ROLE_ADMIN.name())
                    .orElseThrow(() -> new RuntimeException("Role Admin not found. Xin hãy kiểm tra lại initRoles()"));

            // 1. Khởi tạo đối tượng User trước (chưa lưu)
            User adminUser = User.builder()
                    .email("admin@admin.com")
                    .passwordHash(passwordEncoder.encode("admin123"))
                    .firstName("Super")
                    .lastName("Admin")
                    .isActive(true)
                    .userRoles(new HashSet<>()) // Khởi tạo Set rỗng
                    .build();

            // 2. Khởi tạo đối tượng trung gian UserRole
            UserRole userRole = UserRole.builder()
                    .user(adminUser)
                    .role(adminRole)
                    .build();

            // 3. Thêm UserRole vào Set của User
            adminUser.getUserRoles().add(userRole);

            // 4. Lưu User (nhờ CascadeType.ALL trên thuộc tính userRoles, Spring Data JPA sẽ tự động lưu luôn bảng trung gian)
            userRepository.save(adminUser);

            log.info("INIT: Created Admin account (admin@admin.com / admin123)");
        } else {
            log.info("INIT: Admin account already exists");
        }
    }

    private void initPaymentMethods() {
        createPaymentMethodIfAbsent("CASH", "Thanh toán khi nhận hàng");
        createPaymentMethodIfAbsent("VNPAY", "Thanh toán VNPAY");
        createPaymentMethodIfAbsent("WALLET", "Thanh toán bằng ví");
    }

    private void createPaymentMethodIfAbsent(String code, String name) {
        if (!paymentMethodRepository.existsByCode(code)) {
            paymentMethodRepository.save(PaymentMethod.builder()
                    .code(code)
                    .name(name)
                    .isActive(true)
                    .build());
            log.info("INIT: Created payment method {}", code);
        }
    }
}
