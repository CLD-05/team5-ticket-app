package com.example.ticketing.global.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.User;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.security.Key;
import java.util.Collections;
import java.util.Date;

@Component
public class JwtTokenProvider {

    @Value("${jwt.secret}")
    private String secretKey;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private Key key;
    // parser를 매 요청 build 하면 JCA provider 조회가 fat jar UrlJarFiles 캐시 락을 타
    // 고부하에서 스레드가 직렬화된다. 불변·스레드안전 객체이므로 1회만 build 해 재사용.
    private JwtParser parser;

    @PostConstruct
    protected void init() {
        this.key = Keys.hmacShaKeyFor(secretKey.getBytes());
        this.parser = Jwts.parserBuilder().setSigningKey(key).build();
    }

    public String createToken(String userId, String email) {
        Claims claims = Jwts.claims().setSubject(userId);
        claims.put("email", email);

        Date now = new Date();
        Date validity = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(now)
                .setExpiration(validity)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public String getUserIdFromToken(String token) {
        return parser
                .parseClaimsJws(token)
                .getBody()
                .getSubject();
    }

    public boolean validateToken(String token) {
        parser.parseClaimsJws(token);
        // TODO: Redis 블랙리스트 체크 로직 추가 가능
        return true;
    }
}