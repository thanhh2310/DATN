package com.example.demo.service;

import com.example.demo.auth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.demo.auth.JwtUtils;
import com.example.demo.model.User;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;

@Component
@RequiredArgsConstructor
@Slf4j
public class OAuth2LoginSuccessHandler extends SimpleUrlAuthenticationSuccessHandler {
    private final UserService userService;
    private final RedisService redisService;
    private final JwtUtils jwtUtils;
    private final HttpCookieOAuth2AuthorizationRequestRepository httpCookieOAuth2AuthorizationRequestRepository;

    @Value("${frontend.url}")
    private String frontendUrl;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication)
            throws IOException, ServletException {
        httpCookieOAuth2AuthorizationRequestRepository.removeAuthorizationRequestCookies(request, response);
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String firstName = oAuth2User.getAttribute("given_name");
        String lastName = oAuth2User.getAttribute("family_name");

        log.info("Bắt đầu xử lý đăng nhập OAuth2 thành công cho email: {}", email);

        User user = userService.processOAuth2PostLogin(email, firstName, lastName);

        // KIỂM TRA isActive — chặn user bị khóa đăng nhập
        if (!Boolean.TRUE.equals(user.getIsActive())) {
            log.warn("Tài khoản {} đang bị khóa. Từ chối cho phép đăng nhập.", email);
            String errorUrl = UriComponentsBuilder.fromUriString(frontendUrl + "/login")
                    .queryParam("error", "account_disabled")
                    .build().toUriString();
            getRedirectStrategy().sendRedirect(request, response, errorUrl);
            return;
        }
        log.debug("Tiến hành khởi tạo JWT và lưu Refresh Token vào Redis cho user: {}", email);
        String accessToken = jwtUtils.generateAccessToken(user);
        String refreshToken = jwtUtils.generateRefreshToken(user);

        redisService.saveRefreshTokenToRedis(user.getEmail(), refreshToken, jwtUtils.getJwtLongExpiration());

        // Redirect về Frontend dùng URL Fragment (#) thay vì query param
        // Giúp token KHÔNG bị gửi lên server khi FE load lại trang, tăng bảo mật
        // URL sẽ dạng: http://localhost:5173/oauth-redirect#access_token=xyz&refresh_token=abc
        String targetUrl = frontendUrl + "/oauth-redirect?" +
                "access_token=" + accessToken +
                "&refresh_token=" + refreshToken;
        log.info("Hoàn tất quy trình OAuth2. Đang chuyển hướng {} về Frontend.", email);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }
}
