package com.example.demo.config;

import com.example.demo.auth.HttpCookieOAuth2AuthorizationRequestRepository;
import com.example.demo.auth.JwtAuthenticationFilter;
import com.example.demo.auth.JwtUtils;
import com.example.demo.service.OAuth2LoginFailureHandler;
import com.example.demo.service.OAuth2LoginSuccessHandler;
import com.example.demo.service.RedisService;
import com.example.demo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {
    private final OAuth2LoginSuccessHandler oAuth2LoginSuccessHandler;
    private final OAuth2LoginFailureHandler oAuth2LoginFailureHandler;
    private final HttpCookieOAuth2AuthorizationRequestRepository cookieAuthorizationRequestRepository;



    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(JwtUtils jwtUtils,
                                                           UserService userService,
                                                           RedisService redisService){
        return new JwtAuthenticationFilter(jwtUtils, userService, redisService);
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http,
                                                   JwtAuthenticationFilter jwtAuthenticationFilter,
                                                   UserService userService) throws Exception {
        http.cors(Customizer.withDefaults())
                .csrf(csrf -> csrf.disable())
                .sessionManagement(sess-> sess.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .httpBasic(basic -> basic.disable())
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/logout", "/api/auth/change-password",
                                "/api/cart/merge").authenticated()
                        .requestMatchers(
                                "/api/auth/**",
                                "/login/oauth2/**",
                                "/error",
                                "/api/categories/**",
                                "/api/attributes/**",
                                "/api/brands/**",
                                "/api/cart/**"
                        ).permitAll()
                        // Cho phép Guest xem sản phẩm (chỉ GET), POST/PUT/DELETE vẫn cần ADMIN qua @PreAuthorize
                        .requestMatchers(HttpMethod.GET, "/api/products/**", "/api/skus/*/images").permitAll()
                        // VNPAY callbacks: cả return và IPN đều không có JWT token
                        .requestMatchers("/api/payment/vnpay-return/**").permitAll()
                        .requestMatchers("/api/payment/vnpay-ipn/**").permitAll()
                        .anyRequest().authenticated()
                )
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
                )
//                .formLogin(Customizer.withDefaults())
                .oauth2Login(oauth2 -> oauth2
                        .authorizationEndpoint(auth -> auth
                                // Kéo Spring khỏi việc dùng Session mặc định
                                .authorizationRequestRepository(cookieAuthorizationRequestRepository)
                        )
                        .successHandler(oAuth2LoginSuccessHandler)
                        .failureHandler(oAuth2LoginFailureHandler)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);
        return http.build();
    }
}
