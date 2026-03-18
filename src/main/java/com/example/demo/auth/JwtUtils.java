package com.example.demo.auth;

import com.example.demo.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.stereotype.Component;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.*;
import java.util.stream.Collectors;

@Component
@Getter
public class JwtUtils {
    @Value("${jwt.secret}")
    private String privateKey;

    @Value("${jwt.expiration}")
    private long jwtExpiration;

    @Value("${jwt.long.expiration}")
    private long jwtLongExpiration;

    private Key key;

    @PostConstruct
    public void init(){
//        byte[] keyBytes = Decoders.BASE64.decode(privateKey);
        key = Keys.hmacShaKeyFor(privateKey.getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(User user){
        // tao doi tuong claims
        Map<String, Object> claims = new HashMap<>();
        //lay role cua user
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        //dua role cua user vao trong claims
        claims.put("roles", roles);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String generateRefreshToken(User user){
        // tao doi tuong claims
        Map<String, Object> claims = new HashMap<>();
        //lay role cua user
        List<String> roles = user.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.toList());
        //dua role cua user vao trong claims
        claims.put("roles", roles);

        Date now = new Date();
        Date expiry = new Date(now.getTime() + jwtLongExpiration);

        return Jwts.builder()
                .setClaims(claims)
                .setSubject(user.getEmail())
                .setIssuedAt(now)
                .setExpiration(expiry)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public Claims extractClaims(String token){
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public String extractEmail(String token){
        return extractClaims(token).getSubject();
    }

    public Date extractIssuedAt (String token){
        Claims claims = extractClaims(token);
        return claims.getIssuedAt();
    }

    public Date extractExpiration (String token){
        Claims claims = extractClaims(token);
        return claims.getExpiration();
    }

    public List<String> extractRoles (String token){
        Object roleObj = extractClaims(token).get("roles");
        if(roleObj instanceof List<?>){
            return ((List<?>) roleObj ).stream()
                    .map(Object::toString)
                    .collect(Collectors.toList());
        }
        return Collections.emptyList();
    }

    public Boolean isTokenExpired(String token){
        return extractExpiration(token).before(new Date());
    }

    public Boolean validateToken(String token, User user){
        final String email = extractEmail(token);
        return (email.equals(user.getEmail())) && !isTokenExpired(token);
    }

}
