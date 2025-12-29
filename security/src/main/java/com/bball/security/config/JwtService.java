package com.bball.security.config;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Slf4j
@Service
public class JwtService {

    @Value("${application.security.jwt.secret-key:}")
    private String secretKey;

    @PostConstruct
    public void validateSecretKey() {
        if (secretKey == null || secretKey.trim().isEmpty()) {
            log.error("❌ CRITICAL: JWT secret key is not configured!");
            throw new IllegalStateException(
                "JWT secret key is REQUIRED for production. " +
                "Please set APPLICATION_SECURITY_JWT_SECRET_KEY environment variable. " +
                "Generate a secure key using: openssl rand -base64 64"
            );
        }
        
        // Validate Base64 format early
        try {
            Decoders.BASE64.decode(secretKey);
            log.info("✅ JWT secret key configured successfully");
        } catch (IllegalArgumentException e) {
            log.error("❌ CRITICAL: Invalid JWT secret key format!");
            throw new IllegalStateException(
                "Invalid JWT secret key format. The secret must be a valid Base64-encoded string. " +
                "Generate a new one using: openssl rand -base64 64", e
            );
        }
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public String generateToken(UserDetails userDetails) {
        return generateToken(new HashMap<>(),  userDetails);
    }

    public String generateToken(
            Map<String, Object> extractClaims,
            UserDetails userDetails
    ) {
        return Jwts
                .builder()
                .setClaims(extractClaims)
                .setSubject(userDetails.getUsername())
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + 1000 * 60 * 24))
                .signWith(getSignInKey(), SignatureAlgorithm.HS512)
                .compact();
    }

    public boolean isTokenValid(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername())) && !isTokenExpired(token);
    }

    private boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSignInKey())
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private Key getSignInKey() {
        // Validation is done in @PostConstruct, but double-check here for safety
        if (secretKey == null || secretKey.trim().isEmpty()) {
            throw new IllegalStateException("JWT secret key is not configured. Please set APPLICATION_SECURITY_JWT_SECRET_KEY environment variable.");
        }
        try {
            byte[] keyBytes = Decoders.BASE64.decode(secretKey);
            return Keys.hmacShaKeyFor(keyBytes);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException(
                "Invalid JWT secret key format. The secret must be a valid Base64-encoded string. " +
                "Generate a new one using: openssl rand -base64 64", e
            );
        }
    }
}
