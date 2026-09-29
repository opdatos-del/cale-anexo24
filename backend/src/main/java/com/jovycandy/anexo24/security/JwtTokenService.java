package com.jovycandy.anexo24.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;

/**
 * Emisión y validación de tokens JWT.
 *
 * <p>Tokens stateless (ADR-002) con el identificador de usuario y sus
 * permisos; la firma usa {@code HS256} con secreto por ambiente.</p>
 */
@Service
public class JwtTokenService {

    private final SecretKey key;
    private final Duration expiration;

    /**
     * Constructor con valores de configuración.
     *
     * @param secret            secreto de firma (mínimo 32 caracteres)
     * @param expirationMinutes vigencia del token en minutos
     */
    public JwtTokenService(@Value("${jwt.secret}") String secret,
                           @Value("${jwt.expiration-minutes}") long expirationMinutes) {
        if (secret == null || secret.isBlank() || secret.startsWith("${")) {
            throw new IllegalArgumentException("Configuración obligatoria ausente: jwt.secret (JWT_SECRET)");
        }
        byte[] secretBytes = secret.getBytes(StandardCharsets.UTF_8);
        if (secretBytes.length < 32) {
            throw new IllegalArgumentException("jwt.secret (JWT_SECRET) debe tener al menos 32 bytes para HS256");
        }
        if (expirationMinutes <= 0) {
            throw new IllegalArgumentException("jwt.expiration-minutes debe ser mayor que cero");
        }
        this.key = Keys.hmacShaKeyFor(secretBytes);
        this.expiration = Duration.ofMinutes(expirationMinutes);
    }

    /**
     * Genera un token para el usuario y sus permisos.
     *
     * @param userId      identificador del usuario en el esquema app
     * @param username    clave de acceso
     * @param authorities permisos asignados
     * @return token JWT firmado
     */
    public String generateToken(Long userId, String username, List<String> authorities) {
        Instant now = Instant.now();
        return Jwts.builder()
                .subject(username)
                .claim("uid", userId)
                .claim("auth", authorities)
                .issuedAt(Date.from(now))
                .expiration(Date.from(now.plus(expiration)))
                .signWith(key)
                .compact();
    }

    /**
     * Devuelve la vigencia configurada para informar al cliente.
     *
     * @return minutos de vigencia del token
     */
    public long expirationMinutes() {
        return expiration.toMinutes();
    }

    /**
     * Valida y extrae las declaraciones del token.
     *
     * @param token token JWT
     * @return declaraciones si el token es válido
     * @throws JwtException si el token es inválido o expiró
     */
    public Claims parseToken(String token) {
        return Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }
}
