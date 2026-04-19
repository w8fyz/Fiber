package sh.fyz.fiber.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Function;

/**
 * Utility class for handling JWT (JSON Web Token) operations.
 * Uses lazy initialization to avoid static init ordering issues with FiberServer.
 */
public class JwtUtil {

    private static final Logger logger = LoggerFactory.getLogger(JwtUtil.class);

    private static final AtomicReference<State> STATE = new AtomicReference<>();

    private static final Set<String> REVOKED_REFRESH_TOKENS = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private record State(SecretKey key, JwtParser parser, long tokenValidity, long refreshTokenValidity) {}

    private static State ensureInitialized() {
        State existing = STATE.get();
        if (existing != null) return existing;
        synchronized (JwtUtil.class) {
            existing = STATE.get();
            if (existing != null) return existing;
            String secret = System.getenv("FIBER_SECRET_KEY") != null
                    ? System.getenv("FIBER_SECRET_KEY")
                    : FiberServer.get().getConfig().getJwtSecretKey();
            SecretKey key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            JwtParser parser = Jwts.parser().verifyWith(key).build();
            long tokenValidity = System.getenv("FIBER_TOKEN_VALIDITY") != null
                    ? Long.parseLong(System.getenv("FIBER_TOKEN_VALIDITY"))
                    : FiberServer.get().getConfig().getJwtTokenValidity();
            long refreshValidity = System.getenv("FIBER_REFRESH_TOKEN_VALIDITY") != null
                    ? Long.parseLong(System.getenv("FIBER_REFRESH_TOKEN_VALIDITY"))
                    : FiberServer.get().getConfig().getJwtRefreshTokenValidity();
            State s = new State(key, parser, tokenValidity, refreshValidity);
            STATE.set(s);
            return s;
        }
    }

    public static String generateToken(UserAuth userAuth, String ipAddress, String userAgent) {
        return generateToken(userAuth, ipAddress, userAgent, null);
    }

    public static String generateToken(UserAuth userAuth, String ipAddress, String userAgent, String sessionId) {
        State s = ensureInitialized();
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "access");
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }
        return createToken(claims, s.tokenValidity);
    }

    public static String generateRefreshToken(UserAuth userAuth, String ipAddress, String userAgent) {
        return generateRefreshToken(userAuth, ipAddress, userAgent, null);
    }

    public static String generateRefreshToken(UserAuth userAuth, String ipAddress, String userAgent, String sessionId) {
        State s = ensureInitialized();
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "refresh");
        if (sessionId != null) {
            claims.put("sessionId", sessionId);
        }
        return createToken(claims, s.refreshTokenValidity);
    }

    public static String createToken(Map<String, Object> claims, long validity) {
        State s = ensureInitialized();
        return Jwts.builder()
                .claims(claims)
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + validity))
                .signWith(s.key, SignatureAlgorithm.HS256)
                .compact();
    }

    public static Claims validateToken(String token, String ipAddress, String userAgent) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenIp = claims.get("ip", String.class);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            if (!"access".equals(tokenType) || isTokenExpired(claims)) {
                return null;
            }

            if (!Objects.equals(tokenUserAgent, userAgent)) {
                return null;
            }

            // IP binding is strict in production, relaxed in development mode to support
            // local NAT / mobile networks where the client IP legitimately changes.
            if (!isDevMode() && !Objects.equals(tokenIp, ipAddress)) {
                return null;
            }

            return claims;
        } catch (Exception e) {
            return null;
        }
    }

    public static boolean validateRefreshToken(String token, String ipAddress, String userAgent) {
        try {
            if (REVOKED_REFRESH_TOKENS.contains(token)) {
                return false;
            }
            Claims claims = extractAllClaims(token);
            String tokenIp = claims.get("ip", String.class);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            if (!"refresh".equals(tokenType) || isTokenExpired(claims)) {
                return false;
            }

            if (!Objects.equals(tokenUserAgent, userAgent)) {
                return false;
            }

            if (!isDevMode() && !Objects.equals(tokenIp, ipAddress)) {
                return false;
            }

            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Mark a refresh token as revoked. Subsequent calls to
     * {@link #validateRefreshToken(String, String, String)} will return {@code false}.
     * The in-memory set is bounded by the refresh TTL — callers may prune periodically.
     */
    public static void revokeRefreshToken(String token) {
        if (token != null && !token.isBlank()) {
            REVOKED_REFRESH_TOKENS.add(token);
        }
    }

    /** Visible for testing. */
    public static void clearRevokedRefreshTokens() {
        REVOKED_REFRESH_TOKENS.clear();
    }

    private static Claims extractAllClaims(String token) {
        State s = ensureInitialized();
        return s.parser.parseSignedClaims(token).getPayload();
    }

    private static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
    }

    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Safely extract the "id" claim. Returns {@code null} (never throws) on any parsing
     * or signature failure.
     */
    public static Object extractId(String token) {
        try {
            return extractAllClaims(token).get("id");
        } catch (Exception e) {
            logger.debug("Failed to extract id from token: {}", e.getMessage());
            return null;
        }
    }

    /**
     * Safely extract the "sessionId" claim. Returns {@code null} on failure.
     */
    public static String extractSessionId(String token) {
        try {
            return extractAllClaims(token).get("sessionId", String.class);
        } catch (Exception e) {
            return null;
        }
    }

    private static boolean isDevMode() {
        try {
            return FiberServer.get().isDev();
        } catch (IllegalStateException e) {
            return false;
        }
    }
}
