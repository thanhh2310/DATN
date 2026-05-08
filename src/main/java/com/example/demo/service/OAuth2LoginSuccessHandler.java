package com.example.demo.service;

import com.example.demo.Enum.ErrorCode;
import com.example.demo.Enum.RoleName;
import com.example.demo.auth.JwtUtils;
import com.example.demo.config.WebErrorConfig;
import com.example.demo.model.Cart;
import com.example.demo.model.Role;
import com.example.demo.model.User;
import com.example.demo.model.UserRole;
import com.example.demo.repository.CartRepository;
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
    private final CartRepository cartRepository;
    private final RedisService redisService;
    private final JwtUtils jwtUtils;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        User user = userRepository.findByEmail(email).orElseGet(() -> {
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

            // Tạo Cart cho User mới đăng ký qua Google
            cartRepository.save(Cart.builder().user(savedUser).build());

            return savedUser;
        });

        // KIỂM TRA isActive — chặn user bị khóa đăng nhập
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", "account_disabled")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }

        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        redisService.saveRefreshTokenToRedis(user.getEmail(), refreshToken, jwtUtils.getJwtLongExpiration());

        // Redirect về Frontend dùng URL Fragment (#) thay vì query param
        // Giúp token KHÔNG bị gửi lên server khi FE load lại trang, tăng bảo mật
        // URL sẽ dạng: http://localhost:3000/oauth-redirect#access_token=xyz&refresh_token=abc
        String targetUrl = frontendUrl + "/oauth-redirect#" +
                "access_token=" + accessToken +
                "&refresh_token=" + refreshToken;

        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
