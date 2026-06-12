package com.example.demo.config;

import com.example.demo.dto.response.ApiResponse;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class AbuseProtectionFilter extends OncePerRequestFilter {
    private static final long DEFAULT_MAX_BODY_BYTES = 2 * 1024 * 1024;
    private static final long AI_MAX_BODY_BYTES = 64 * 1024;
    private static final long CHECKOUT_MAX_BODY_BYTES = 256 * 1024;
    private static final Duration CLEANUP_INTERVAL = Duration.ofMinutes(5);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Map<String, Bucket> buckets = new ConcurrentHashMap<>();
    private volatile long lastCleanupAt = System.currentTimeMillis();

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {
        if (isPreflight(request)) {
            filterChain.doFilter(request, response);
            return;
        }

        Rule rule = resolveRule(request);
        if (isBodyTooLarge(request, rule.maxBodyBytes())) {
            writeError(response, HttpStatus.PAYLOAD_TOO_LARGE, 413, "Request body is too large");
            return;
        }

        cleanupIfNeeded();

        String identity = resolveIdentity(request, rule.scope());
        String key = rule.name() + ":" + identity;
        Bucket bucket = buckets.computeIfAbsent(key, ignored -> new Bucket(rule.maxRequests(), rule.windowMillis()));

        if (!bucket.tryConsume(rule.maxRequests(), rule.windowMillis())) {
            response.setHeader(HttpHeaders.RETRY_AFTER, String.valueOf(rule.windowMillis() / 1000));
            writeError(response, HttpStatus.TOO_MANY_REQUESTS, 429, "Too many requests. Please try again later.");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private Rule resolveRule(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();

        if (path.startsWith("/api/ai/chat")) {
            return new Rule("ai-chat", Scope.PRINCIPAL_OR_IP, 12, Duration.ofMinutes(1), AI_MAX_BODY_BYTES);
        }

        if (path.startsWith("/api/wishlist/check")) {
            return new Rule("wishlist-check", Scope.PRINCIPAL_OR_IP, 600, Duration.ofMinutes(1), DEFAULT_MAX_BODY_BYTES);
        }

        if (path.startsWith("/api/auth/forgot-password")
                || path.startsWith("/api/auth/verify")
                || path.startsWith("/api/auth/reset-password")) {
            return new Rule("auth-otp", Scope.IP, 50, Duration.ofMinutes(5), DEFAULT_MAX_BODY_BYTES);
        }

        if (path.startsWith("/api/auth/login")
                || path.startsWith("/api/auth/register")) {
            return new Rule("auth-entry", Scope.IP, 50, Duration.ofMinutes(1), DEFAULT_MAX_BODY_BYTES);
        }

        if (path.startsWith("/api/orders")
                || path.startsWith("/api/checkout")
                || path.startsWith("/api/payment")
                || path.startsWith("/api/wallet")) {
            return new Rule("business-mutation", Scope.PRINCIPAL_OR_IP, 50, Duration.ofMinutes(1), CHECKOUT_MAX_BODY_BYTES);
        }

        if (isUnsafeMethod(method)) {
            return new Rule("unsafe-api", Scope.PRINCIPAL_OR_IP, 50, Duration.ofMinutes(1), DEFAULT_MAX_BODY_BYTES);
        }

        return new Rule("read-api", Scope.IP, 180, Duration.ofMinutes(1), DEFAULT_MAX_BODY_BYTES);
    }

    private boolean isBodyTooLarge(HttpServletRequest request, long maxBodyBytes) {
        long contentLength = request.getContentLengthLong();
        return contentLength > maxBodyBytes;
    }

    private String resolveIdentity(HttpServletRequest request, Scope scope) {
        if (scope == Scope.PRINCIPAL_OR_IP) {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && authentication.getName() != null) {
                return "user:" + authentication.getName().toLowerCase(Locale.ROOT);
            }
        }
        return "ip:" + resolveClientIp(request);
    }

    private String resolveClientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }

        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }

        return request.getRemoteAddr();
    }

    private boolean isUnsafeMethod(String method) {
        return HttpMethod.POST.matches(method)
                || HttpMethod.PUT.matches(method)
                || HttpMethod.PATCH.matches(method)
                || HttpMethod.DELETE.matches(method);
    }

    private boolean isPreflight(HttpServletRequest request) {
        return HttpMethod.OPTIONS.matches(request.getMethod());
    }

    private void cleanupIfNeeded() {
        long now = System.currentTimeMillis();
        if (now - lastCleanupAt < CLEANUP_INTERVAL.toMillis()) {
            return;
        }

        lastCleanupAt = now;
        buckets.entrySet().removeIf(entry -> entry.getValue().isExpired(now));
    }

    private void writeError(HttpServletResponse response,
                            HttpStatus status,
                            int code,
                            String message) throws IOException {
        response.setStatus(status.value());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        objectMapper.writeValue(response.getWriter(), ApiResponse.builder()
                .code(code)
                .message(message)
                .build());
    }

    private enum Scope {
        IP,
        PRINCIPAL_OR_IP
    }

    private record Rule(String name, Scope scope, int maxRequests, Duration window, long maxBodyBytes) {
        long windowMillis() {
            return window.toMillis();
        }
    }

    private static final class Bucket {
        private int remaining;
        private long resetAt;
        private long lastSeenAt;

        private Bucket(int maxRequests, long windowMillis) {
            long now = System.currentTimeMillis();
            this.remaining = maxRequests;
            this.resetAt = now + windowMillis;
            this.lastSeenAt = now;
        }

        synchronized boolean tryConsume(int maxRequests, long windowMillis) {
            long now = System.currentTimeMillis();
            lastSeenAt = now;

            if (now >= resetAt) {
                remaining = maxRequests;
                resetAt = now + windowMillis;
            }

            if (remaining <= 0) {
                return false;
            }

            remaining--;
            return true;
        }

        boolean isExpired(long now) {
            return now - lastSeenAt > CLEANUP_INTERVAL.toMillis() * 2;
        }
    }
}
