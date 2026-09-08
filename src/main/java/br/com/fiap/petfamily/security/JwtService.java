package br.com.fiap.petfamily.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.Map;

@Service
public class JwtService {

    private final Key signingKey;
    private final long expirationMs;

    public JwtService(@Value("${jwt.secret}") String secret,
                       @Value("${jwt.expiration-ms}") long expirationMs) {
        this.signingKey = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expirationMs = expirationMs;
    }

    public String gerarToken(Long usuarioId, String email, String role, Long tutorId) {
        Date agora = new Date();
        Date expiracao = new Date(agora.getTime() + expirationMs);

        var builder = Jwts.builder()
                .setSubject(email)
                .claim("usuarioId", usuarioId)
                .claim("role", role)
                .setIssuedAt(agora)
                .setExpiration(expiracao)
                .signWith(signingKey, SignatureAlgorithm.HS256);

        if (tutorId != null) {
            builder.claim("tutorId", tutorId);
        }
        return builder.compact();
    }

    public long getExpirationMs() {
        return expirationMs;
    }

    public Claims parseClaims(String token) throws JwtException {
        return Jwts.parserBuilder()
                .setSigningKey(signingKey)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    public boolean isTokenValido(String token) {
        try {
            parseClaims(token);
            return true;
        } catch (JwtException | IllegalArgumentException e) {
            return false;
        }
    }

    public String extrairEmail(String token) {
        return parseClaims(token).getSubject();
    }

    public Map<String, Object> extrairClaims(String token) {
        return parseClaims(token);
    }
}
