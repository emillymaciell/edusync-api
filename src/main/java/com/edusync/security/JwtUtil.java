package com.edusync.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.util.Date;
import java.util.function.Function;

/**
 * Utilitário para geração e validação de tokens JWT (API jjwt 0.12.x).
 * A chave e o tempo de expiração vêm do application.properties.
 */
@Component
public class JwtUtil {

    private final SecretKey signingKey;
    private final long expirationMs;
    private final String issuer;

    public JwtUtil(
            @Value("${edusync.jwt.secret}") String secret,
            @Value("${edusync.jwt.expiration-ms}") long expirationMs,
            @Value("${edusync.jwt.issuer}") String issuer) {
        // A secret é Base64; decodifica para obter os bytes da chave HMAC.
        byte[] keyBytes = Decoders.BASE64.decode(secret);
        this.signingKey = Keys.hmacShaKeyFor(keyBytes);
        this.expirationMs = expirationMs;
        this.issuer = issuer;
    }

    /** Gera um token assinado contendo o e-mail (subject), id e role como claims. */
    public String generateToken(String email, Long userId, String role) {
        Date now = new Date();
        Date expiry = new Date(now.getTime() + expirationMs);
        return Jwts.builder()
                .subject(email)
                .claim("userId", userId)
                .claim("role", role)
                .issuer(issuer)
                .issuedAt(now)
                .expiration(expiry)
                .signWith(signingKey) // algoritmo inferido a partir do tamanho da chave HMAC
                .compact();
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public <T> T extractClaim(String token, Function<Claims, T> resolver) {
        return resolver.apply(parseClaims(token));
    }

    /** Valida assinatura, expiração e correspondência do subject. */
    public boolean isTokenValid(String token, String expectedUsername) {
        try {
            final String username = extractUsername(token);
            return username.equals(expectedUsername) && !isExpired(token);
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isExpired(String token) {
        return extractClaim(token, Claims::getExpiration).before(new Date());
    }

    private Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
