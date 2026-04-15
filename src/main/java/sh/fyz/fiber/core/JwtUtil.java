package sh.fyz.fiber.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for handling JWT (JSON Web Token) operations.
 * Uses lazy initialization to avoid static init ordering issues with FiberServer.
 */
public class JwtUtil {

    private static volatile Key key;
    private static volatile JwtParser cachedParser;
    private static volatile long tokenValidity;
    private static volatile long refreshTokenValidity;
    private static volatile boolean initialized = false;

    private static void ensureInitialized() {
        if (!initialized) {
            synchronized (JwtUtil.class) {
                if (!initialized) {
                    String secret = System.getenv("FIBER_SECRET_KEY") != null
                            ? System.getenv("FIBER_SECRET_KEY")
                            : FiberServer.get().getConfig().getJwtSecretKey();
                    key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
                    cachedParser = Jwts.parser().setSigningKey(key).build();
                    tokenValidity = System.getenv("FIBER_TOKEN_VALIDITY") != null
                            ? Long.parseLong(System.getenv("FIBER_TOKEN_VALIDITY"))
                            : FiberServer.get().getConfig().getJwtTokenValidity();
                    refreshTokenValidity = System.getenv("FIBER_REFRESH_TOKEN_VALIDITY") != null
                            ? Long.parseLong(System.getenv("FIBER_REFRESH_TOKEN_VALIDITY"))
                            : FiberServer.get().getConfig().getJwtRefreshTokenValidity();
                    initialized = true;
                }
            }
        }
    }

    public static String generateToken(UserAuth userAuth, String ipAddress, String userAgent) {
        return generateToken(userAuth, ipAddress, userAgent, null);
    }

    public static String generateToken(UserAuth userAuth, String ipAddress, String userAgent, String sessionId) {
        ensureInitialized();
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "access");
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }
        return createToken(claims, tokenValidity);
    }

    public static String generateRefreshToken(UserAuth userAuth, String ipAddress, String userAgent) {
        return generateRefreshToken(userAuth, ipAddress, userAgent, null);
    }

    public static String generateRefreshToken(UserAuth userAuth, String ipAddress, String userAgent, String sessionId) {
        ensureInitialized();
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "refresh");
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }
        return createToken(claims, refreshTokenValidity);
    }

    public static String createToken(Map<String, Object> claims, long validity) {
        ensureInitialized();
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims validateToken(String token, String ipAddress, String userAgent) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            if (!"access".equals(tokenType) || isTokenExpired(claims)) {
                return null;
            }

            if (!tokenUserAgent.equals(userAgent)) {
                return null;
            }
            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validateRefreshToken(String token, String ipAddress, String userAgent) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenIp = claims.get("ip", String.class);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            if (!"refresh".equals(tokenType) || isTokenExpired(claims)) {
                return false;
            }

            return tokenIp.equals(ipAddress) && tokenUserAgent.equals(userAgent);
        } catch (Exception e) {
            return false;
        }
    }

    private static Claims extractAllClaims(String token) {
        ensureInitialized();
        return cachedParser
                .parseClaimsJws(token)
                .getBody();
    }

    private static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    public static Object extractId(String token) {
        return extractAllClaims(token).get("id");
    }
}
