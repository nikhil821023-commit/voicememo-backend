package com.voicememo.security;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;

@Component
public class JwtUtil {

    @Value("${app.jwt.secret}")
    private String secret;

    @Value("${app.jwt.expiry-ms:86400000}") // 24h default
    private long expiryMs;

    private SecretKey key() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }

    public String generate(String email, Long userId, String plan) {
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("plan", plan)
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expiryMs))
                .signWith(key())
                .compact();
    }

    public Claims parse(String token) {
        return Jwts.parser()
                .verifyWith(key())
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public boolean isValid(String token) {
        try {
            parse(token);
            return true;
        } catch (JwtException e) {
            return false;
        }
    }

    public String getEmail(String token) {
        return parse(token).getSubject();
    }

    public Long getUserId(String token) {
        return parse(token).get("userId", Long.class);
    }

    public String getPlan(String token) {
        return parse(token).get("plan", String.class);
    }
}