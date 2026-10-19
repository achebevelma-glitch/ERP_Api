package com.example.ERP_Api.Service;


import com.example.ERP_Api.Entity.Role;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
public class JwtService {

    private final SecretKey signingKey = Keys.secretKeyFor(io.jsonwebtoken.SignatureAlgorithm.HS256);

    private static final long EXPIRATION_MS = 1000 * 60 * 60 * 24; // 24 hours

    public String generateToken(String email, Role role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + EXPIRATION_MS);

        return Jwts.builder()
                .subject(email)
                .issuedAt(now)
                .expiration(expiry)
                .claim("role", role.name())
                .signWith(signingKey)
                .compact();
    }
    public String extractRole(String token) {
        Object role = parseClaims(token).get("role");
        return role == null ? "USER" : role.toString();
    }

    public String extractEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public boolean isTokenValid(String token, String email) {
        try {
            Claims claims = parseClaims(token);
            boolean notExpired = claims.getExpiration().after(new Date());
            return claims.getSubject().equals(email) && notExpired;
        } catch (Exception e) {
            return false;
        }
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
