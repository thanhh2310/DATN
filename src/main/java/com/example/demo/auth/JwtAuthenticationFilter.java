package com.example.demo.auth;

import com.example.demo.model.User;
import com.example.demo.service.RedisService;
import com.example.demo.service.UserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtUtils jwtUtils;
    private final UserService userService;
    private final RedisService redisService;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain)
            throws ServletException, IOException
    {
//        if (request.getServletPath().startsWith("/auth/")) {
//            filterChain.doFilter(request, response);
//            return;
//        }
        final String authHeader = request.getHeader(HttpHeaders.AUTHORIZATION);
        String token = null;
        String email = null; // Đổi tên biến cho rõ ràng
        try {
            if(authHeader != null && authHeader.startsWith("Bearer ")){
                token = authHeader.substring(7);
                email = jwtUtils.extractEmail(token);
            }

            if(email != null && SecurityContextHolder.getContext().getAuthentication() == null){
                User user =(User) userService.loadUserByUsername(email);
                if(jwtUtils.validateToken(token, user) && !redisService.isTokenBlacklisted(token) ){
                    List<String> roles = jwtUtils.extractRoles(token);
                    List<SimpleGrantedAuthority> authorities =
                            roles.stream().map(SimpleGrantedAuthority::new).toList();
                    UsernamePasswordAuthenticationToken authToken =
                            new UsernamePasswordAuthenticationToken(email, null, authorities);
                    authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authToken);
                }
            }
            filterChain.doFilter(request, response);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
