package sh.fyz.fiber.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Utility class for handling JWT (JSON Web Token) operations.
 */
public class JwtUtil {
    private static final String SECRET_KEY = "your-secret-key-should-be-very-long-and-secure-in-production";
    private static final long TOKEN_VALIDITY = 3600000; // 1 hour in milliseconds
    private static final Key key = Keys.hmacShaKeyFor(SECRET_KEY.getBytes());

    /**
     * Generate a JWT token for a user
     * @param userAuth The user to generate the token for
     * @return The generated JWT token
     */
    public static String generateToken(UserAuth userAuth) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("username", userAuth.getUsername());
        claims.put("role", userAuth.getRole());
        return createToken(claims);
    }

    /**
     * Create a JWT token with the given claims
     * @param claims The claims to include in the token
     * @return The generated JWT token
     */
    private static String createToken(Map<String, Object> claims) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + TOKEN_VALIDITY))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate a JWT token
     * @param token The token to validate
     * @return true if the token is valid, false otherwise
     */
    public static boolean validateToken(String token) {
        try {
            Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Extract all claims from a token
     * @param token The token to extract claims from
     * @return The claims contained in the token
     */
    public static Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    /**
     * Extract a specific claim from a token
     * @param token The token to extract the claim from
     * @param claimsResolver The function to resolve the claim
     * @return The extracted claim
     */
    public static <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
        final Claims claims = extractAllClaims(token);
        return claimsResolver.apply(claims);
    }

    /**
     * Extract the user ID from a token
     * @param token The token to extract the ID from
     * @return The user ID
     */
    public static String extractId(String token) {
        return extractClaim(token, claims -> claims.get("id", String.class));
    }

    /**
     * Extract the username from a token
     * @param token The token to extract the username from
     * @return The username
     */
    public static String extractUsername(String token) {
        return extractClaim(token, claims -> claims.get("username", String.class));
    }

    /**
     * Extract the role from a token
     * @param token The token to extract the role from
     * @return The role
     */
    public static String extractRole(String token) {
        return extractClaim(token, claims -> claims.get("role", String.class));
    }
} 