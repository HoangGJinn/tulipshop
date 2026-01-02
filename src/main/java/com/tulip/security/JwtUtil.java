package com.tulip.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import javax.crypto.SecretKey;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

@Component
public class JwtUtil {
    
    @Value("${jwt.secret}")
    private String secret;
    
    @Value("${jwt.access-token.expiration}")
    private Long accessTokenExpiration;
    
    @Value("${jwt.refresh-token.expiration}")
    private Long refreshTokenExpiration;
    
    // Cấu hình key cho JWT
    // Hàm này dùng để tạo key cho JWT
    private SecretKey getSigningKey() {
        return Keys.hmacShaKeyFor(secret.getBytes());
    }
    
    // Hàm này dùng để lấy username từ JWT
    public String extractUsername(String token) {
        return extractClaim(token, Claims::getSubject);
    }
    
    // Do userId là cái ta tự định nghĩa trong JWT
    // Nên ta cần lấy nó ra từ Claims
    public Long extractUserId(String token) {
        Claims claims = extractAllClaims(token); // 1. Mở token ra để lấy claims
        return claims.get("userId", Long.class);
    }
    
    public String extractTokenType(String token) {
        Claims claims = extractAllClaims(token); // 1. Mở token ra để lấy claims
        return claims.get("type", String.class); // 2. Tìm nhãn "type" trong claims, có thể là "ACCESS" hoặc "REFRESH"
    }
    
    public String extractRole(String token) {
        Claims claims = extractAllClaims(token);
        return claims.get("role", String.class);
    }
    
    // Hàm này dùng để lấy thời gian hết hạn của JWT
    public Date extractExpiration(String token) {
        return extractClaim(token, Claims::getExpiration);
    }
    
    // <T> T nghĩa là generic type, có thể là String, Long, Date, ...
    // Nếu muốn lấy Username -> String
    // Nếu muốn lấy userId -> Long
    // Nếu muốn lấy thời gian hết hạn -> Date=
    public <T> T extractClaim(String token, Function<Claims, T> claimsResolver) 
    //ClaimsResolver là hàm lambda để lấy giá trị từ Claims
    // T là cái mà mình muốn lấy ra từ Claims
    {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }
    
    private Claims extractAllClaims(String token) {
        return Jwts.parser()
                .verifyWith(getSigningKey()) // 1. Verify với key, nếu không khớp thì sẽ throw exception
                .build() // 2. Build parser
                .parseSignedClaims(token) // 3. Parse token
                .getPayload(); // lấy phần body/payload của token như username, userId, ...
    }
    
    private Boolean isTokenExpired(String token) {
        return extractExpiration(token).before(new Date());
    }
    
    // Generate Access Token (1 giờ)
    public String generateAccessToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "ACCESS");
        // Thêm role vào JWT từ authorities của UserDetails
        String role = userDetails.getAuthorities().stream()
            .findFirst()
            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
            .orElse("CUSTOMER");
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername(), accessTokenExpiration);
    }
    
    // Generate Refresh Token (7 ngày)
    public String generateRefreshToken(UserDetails userDetails, Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        claims.put("type", "REFRESH");
        // Thêm role vào refresh token để có thể validate khi refresh
        String role = userDetails.getAuthorities().stream()
            .findFirst()
            .map(authority -> authority.getAuthority().replace("ROLE_", ""))
            .orElse("CUSTOMER");
        claims.put("role", role);
        return createToken(claims, userDetails.getUsername(), refreshTokenExpiration);
    }
    
    private String createToken(Map<String, Object> claims, String subject, Long expiration) {
        return Jwts.builder()
                .claims(claims)
                .subject(subject)
                .issuedAt(new Date(System.currentTimeMillis()))
                // Vì expiration là số giây, nhưng System.currentTimeMillis() trả về số miligiây
                // nên ta phải nhân cho 1000 để đổi về số giây
                .expiration(new Date(System.currentTimeMillis() + expiration * 1000))
                .signWith(getSigningKey()) // Chèn key vào token
                .compact(); // Tạo token
    }
    
    public Boolean validateToken(String token, UserDetails userDetails) {
        try {
            final String username = extractUsername(token);
            final String tokenType = extractTokenType(token);
            
            // Chỉ validate ACCESS token với userDetails
            if (!"ACCESS".equals(tokenType)) {
                return false;
            }
            
            return (username.equals(userDetails.getUsername()) && !isTokenExpired(token));
        } catch (Exception e) {
            return false;
        }
    }
    
    public Boolean validateRefreshToken(String token) {
        try {
            final String tokenType = extractTokenType(token);
            return "REFRESH".equals(tokenType) && !isTokenExpired(token);
        } catch (Exception e) {
            return false;
        }
    }
    
    // Validate JWT token từ HTTP request
    // Extract token từ Authorization header hoặc cookie, sau đó validate
    public Boolean validateJwtToken(HttpServletRequest request, UserDetails userDetails) {
        if (userDetails == null) {
            return false;
        }
        
        try {
            // Lấy JWT token từ header hoặc cookie
            String jwt = extractTokenFromRequest(request);
            
            if (jwt == null) {
                return false;
            }
            
            // Validate token bằng các hàm có sẵn
            String tokenType = extractTokenType(jwt);
            if (!"ACCESS".equals(tokenType)) {
                return false;
            }
            
            // Validate token với userDetails (hàm này đã kiểm tra hết hạn bên trong)
            return validateToken(jwt, userDetails);
            
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Extract JWT token từ HTTP request
     * Ưu tiên đọc từ Authorization header, nếu không có thì đọc từ cookie
     */
    public String extractTokenFromRequest(HttpServletRequest request) {
        // 1. Ưu tiên đọc từ Authorization header (cho API)
        String authHeader = request.getHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        
        // 2. Nếu không có header, đọc từ cookie (cho web)
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("accessToken".equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        
        return null;
    }
}


