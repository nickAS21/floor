package org.nickas21.smart.security.controller;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import org.nickas21.smart.security.configuration.JwtConnectionProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
@EnableConfigurationProperties({JwtConnectionProperties.class})
public class JwtUtil {

    private static final SecretKey SECRET_KEY = Jwts.SIG.HS256.key().build();

    private final long EXPIRES_IN;

    public JwtUtil(JwtConnectionProperties jwtConnectionProperties) {
        this.EXPIRES_IN = Long.parseLong(jwtConnectionProperties.getExpiresInSec());
    }

    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }

    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }

    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(SECRET_KEY)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }

    public JwtToken generateToken(UserDetails userDetails) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("authorities", userDetails.getAuthorities());
        return createToken(claims, userDetails.getUsername());
    }

    private JwtToken createToken(Map<String, Object> claims, String userName) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime exp = now.plusMinutes(this.EXPIRES_IN / 60);
        LocalDateTime expRefresh = exp.plusMinutes(this.EXPIRES_IN / 60);
        Instant instantNow = now.atZone(ZoneId.systemDefault()).toInstant();
        Instant instantExp = exp.atZone(ZoneId.systemDefault()).toInstant();
        Instant instantRefresh = expRefresh.atZone(ZoneId.systemDefault()).toInstant();
        Date issuedAt = Date.from(instantNow);
        Date jwtExpiry = Date.from(instantExp);
        Date jwtRefresh = Date.from(instantRefresh);
        JwtToken jwtToken = new JwtToken();
        jwtToken.setAccessToken(Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(issuedAt)
                .setExpiration(jwtExpiry)
                .signWith(SECRET_KEY)
                .compact());
       jwtToken.setRefreshToken(Jwts.builder()
                .setClaims(claims)
                .setSubject(userName)
                .setIssuedAt(issuedAt)
                .setExpiration(jwtRefresh)
                .signWith(SECRET_KEY)
                .compact());

        jwtToken.setExpiresTimeInSec(instantExp.getEpochSecond());
        return jwtToken;
    }

    public Boolean validateToken(String token, UserDetails userDetails) {
        final String username = extractUsername(token);
        return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
    }
}

