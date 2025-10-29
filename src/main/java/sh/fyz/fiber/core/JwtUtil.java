package sh.fyz.fiber.core;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

/**
 * Utility class for handling JWT (JSON Web Token) operations.
 */
public class JwtUtil {
    private static final String SECRET_KEY = System.getenv("FIBER_SECRET_KEY") != null ? System.getenv("FIBER_SECRET_KEY") : FiberServer.get().getConfig().getJwtSecretKey();
    private static final long TOKEN_VALIDITY = System.getenv("FIBER_TOKEN_VALIDITY") != null ? Long.parseLong(System.getenv("FIBER_TOKEN_VALIDITY")) : FiberServer.get().getConfig().getJwtTokenValidity();
    private static final long REFRESH_TOKEN_VALIDITY =  System.getenv("FIBER_REFRESH_TOKEN_VALIDITY") != null ? Long.parseLong(System.getenv("FIBER_REFRESH_TOKEN_VALIDITY")) : FiberServer.get().getConfig().getJwtRefreshTokenValidity();
    private static final Key key = Keys.hmacShaKeyFor((SECRET_KEY).getBytes());

    /**
     * Generate a JWT token for a user
     * @param userAuth The user to generate the token for
     * @param ipAddress The IP address from which the token is generated
     * @param userAgent The user agent from which the token is generated
     * @return The generated JWT token
     */
    public static String generateToken(UserAuth userAuth, String ipAddress, String userAgent) {
        System.out.println("Genereting token for user ID: " + userAuth.getId()+", with "+ipAddress+" and "+userAgent);
        for(Map.Entry<Object, Object> property : System.getProperties().entrySet()) {
            System.out.println("System Property: " + property.getKey() + " = " + property.getValue());
        }

        System.out.println("SECRET_KEY: " + SECRET_KEY);
        System.out.println("TOKEN_VALIDITY: " + TOKEN_VALIDITY);
        System.out.println("REFRESH_TOKEN_VALIDITY: " + REFRESH_TOKEN_VALIDITY);

        System.out.println("---- JWT --");
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "access");

        for(Map.Entry<String, Object> c :
                claims.entrySet()) {
            System.out.println(c.getKey() + ": " + c.getValue());
        }
        System.out.println("---- JWT --");
        return createToken(claims, TOKEN_VALIDITY);
    }

    /**
     * Generate a refresh token for a user
     * @param userAuth The user to generate the token for
     * @param ipAddress The IP address from which the token is generated
     * @param userAgent The user agent from which the token is generated
     * @return The generated refresh token
     */
    public static String generateRefreshToken(UserAuth userAuth, String ipAddress, String userAgent) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("id", userAuth.getId());
        claims.put("ip", ipAddress);
        claims.put("userAgent", userAgent);
        claims.put("type", "refresh");
        return createToken(claims, REFRESH_TOKEN_VALIDITY);
    }

    /**
     * Create a JWT token with the given claims
     * @param claims The claims to include in the token
     * @param validity The validity period in milliseconds
     * @return The generated JWT token
     */
    public static String createToken(Map<String, Object> claims, long validity) {
        return Jwts.builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + validity))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    /**
     * Validate a JWT token
     * @param token The token to validate
     * @param ipAddress The IP address from which the token is validated
     * @param userAgent The user agent from which the token is validated
     * @return true if the token is valid, false otherwise
     */
    public static boolean validateToken(String token, String ipAddress, String userAgent) {
        try {
            Claims claims = extractAllClaims(token);
            for(Map.Entry<String, Object> c :
                    claims.entrySet()) {
                System.out.println(c.getKey() + ": " + c.getValue());
            }
            String tokenIp = claims.get("ip", String.class);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            // Validate token type and expiration
            if (!"access".equals(tokenType) || isTokenExpired(claims)) {
                System.out.println("Token is expired or type mismatch");
                return false;
            }

            // Validate IP and User-Agent
            System.out.println("tokenIp : "+ipAddress+" and "+tokenIp);
            System.out.println("tokenUserAgent : "+userAgent+" and "+tokenUserAgent);
            //We remove the IP check for now as it can cause issues with users behind proxies or changing networks
            return /*tokenIp.equals(ipAddress) &&*/ tokenUserAgent.equals(userAgent);
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * Validate a refresh token
     * @param token The refresh token to validate
     * @param ipAddress The IP address from which the token is validated
     * @param userAgent The user agent from which the token is validated
     * @return true if the token is valid, false otherwise
     */
    public static boolean validateRefreshToken(String token, String ipAddress, String userAgent) {
        try {
            Claims claims = extractAllClaims(token);
            String tokenIp = claims.get("ip", String.class);
            String tokenUserAgent = claims.get("userAgent", String.class);
            String tokenType = claims.get("type", String.class);

            // Validate token type and expiration
            if (!"refresh".equals(tokenType) || isTokenExpired(claims)) {
                return false;
            }

            // Validate IP and User-Agent
            return tokenIp.equals(ipAddress) && tokenUserAgent.equals(userAgent);
        } catch (Exception e) {
            return false;
        }
    }

    private static Claims extractAllClaims(String token) {
        return Jwts.parser()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    private static boolean isTokenExpired(Claims claims) {
        return claims.getExpiration().before(new Date());
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
     * @return The user ID as an Object (can be Integer or String)
     */
    public static Object extractId(String token) {
        return extractAllClaims(token).get("id");
    }
} 