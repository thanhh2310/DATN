package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.Enum.RoleName;
import com.example.demo.auth.JwtUtils;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.RoleRepository;
import com.example.demo.repository.UserRepository;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final RedisService redisService;
    private final JwtUtils jwtUtils;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        // Lấy thông tin User từ Google
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        // Kiểm tra hoặc Tạo mới User trong DB
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            Role userRole = roleRepository.findByName(RoleName.ROLE_USER.name())
                    .orElseThrow(() -> new WebErrorConfig(ErrorCode.ROLE_NOT_FOUND));

            //Tạo entity User (Chưa lưu DB)
            User newUser = User.builder()
                    .email(email)
                    .firstName(firstName)
                    .lastName(lastName)
                    .isActive(true)
                    .passwordHash("")
                    .userRoles(new HashSet<>()) // Khởi tạo danh sách rỗng
                    .build();

            // Tạo entity bảng trung gian UserRole
            UserRole mapping = UserRole.builder()
                    .user(newUser)
                    .role(userRole)
                    .build();

            // Nạp bảng trung gian vào list của User
            newUser.getUserRoles().add(mapping);

            // Lưu User xuống database
            return userRepository.save(newUser);
        });

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        redisService.saveRefreshTokenToRedis(refreshToken, jwtUtils.getJwtLongExpiration());

        // Redirect về Frontend kèm Token
        // URL sẽ dạng: http://localhost:3000/oauth-redirect?access_token=xyz&refresh_token=abc
        String targetUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/oauth-redirect")
                .queryParam("access_token", accessToken)
                .queryParam("refresh_token", refreshToken)
                .build().toUriString();

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
