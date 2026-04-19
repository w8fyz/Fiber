package sh.fyz.fiber.middleware.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.middleware.Middleware;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

/**
 * CSRF protection via double-submit cookie + Origin/Referer validation.
 *
 * <p>The CSRF token is an HMAC-signed nonce stored in the {@code XSRF-TOKEN} cookie and
 * echoed by the client in the {@code X-CSRF-TOKEN} header for state-changing requests.
 * The token is <b>stable</b> across requests (not regenerated on every mutation) and is
 * rotated only when {@link #rotateToken(HttpServletResponse)} is called explicitly —
 * typically right after a successful login.</p>
 *
 * <p>In addition to the double-submit check, the middleware validates the {@code Origin}
 * header (falling back to {@code Referer}) against the configured CORS allow-list. This
 * prevents a compromised subdomain from issuing cross-origin write requests even if it
 * can plant an XSRF cookie.</p>
 */
public class CsrfMiddleware implements Middleware {

    private static final Logger logger = LoggerFactory.getLogger(CsrfMiddleware.class);

    private static final String CSRF_HEADER = "X-CSRF-TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Set<String> SAFE_METHODS = new HashSet<>(Set.of("GET", "HEAD", "OPTIONS"));
    private static final String HMAC_ALGO = "HmacSHA256";

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = req.getMethod();

        if (!SAFE_METHODS.contains(method)) {
            if (!isOriginOrRefererAllowed(req)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN,
                        "Invalid CSRF origin");
                return false;
            }

            String headerToken = req.getHeader(CSRF_HEADER);
            String cookieToken = getCsrfTokenFromCookie(req);

            if (headerToken == null || cookieToken == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }

            if (!verifyToken(headerToken) || !constantTimeEquals(headerToken, cookieToken)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }
            // Token is valid — we do NOT rotate here. Callers rotate explicitly at login.
        } else {
            String existingToken = getCsrfTokenFromCookie(req);
            if (existingToken == null || !verifyToken(existingToken)) {
                rotateToken(resp);
            }
        }

        return true;
    }

    private static boolean isOriginOrRefererAllowed(HttpServletRequest req) {
        CorsService cors;
        try {
            cors = FiberServer.get().getCorsService();
        } catch (Exception e) {
            // No CORS configured — fall back to permissive behaviour (fail-open for CSRF origin check).
            return true;
        }

        boolean dev = safeIsDev();

        String origin = req.getHeader("Origin");
        if (origin != null && !origin.isBlank() && !"null".equals(origin)) {
            if (cors.isOriginAllowed(origin)) {
                return true;
            }
            if (dev) {
                logger.warn("[CSRF] Allowing unlisted Origin '{}' in dev mode", origin);
                return true;
            }
            logger.debug("[CSRF] Rejecting request: Origin '{}' not allowed", origin);
            return false;
        }

        String referer = req.getHeader("Referer");
        if (referer != null && !referer.isBlank()) {
            String refOrigin = extractOrigin(referer);
            if (refOrigin != null && cors.isOriginAllowed(refOrigin)) {
                return true;
            }
            if (dev) {
                logger.warn("[CSRF] Allowing unlisted Referer '{}' in dev mode", referer);
                return true;
            }
            logger.debug("[CSRF] Rejecting request: Referer '{}' not allowed", referer);
            return false;
        }

        if (dev) {
            logger.warn("[CSRF] No Origin/Referer header present — allowing in dev mode");
            return true;
        }
        logger.debug("[CSRF] Rejecting request: missing Origin and Referer");
        return false;
    }

    private static String extractOrigin(String url) {
        try {
            URI uri = new URI(url);
            if (uri.getScheme() == null || uri.getHost() == null) return null;
            StringBuilder sb = new StringBuilder(uri.getScheme()).append("://").append(uri.getHost());
            if (uri.getPort() > 0) sb.append(':').append(uri.getPort());
            return sb.toString();
        } catch (URISyntaxException e) {
            return null;
        }
    }

    private static boolean safeIsDev() {
        try {
            return FiberServer.get().isDev();
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * Generate a fresh CSRF token and set it in the response. Call this explicitly after
     * any privilege-changing operation (login, logout, role change).
     */
    public static void rotateToken(HttpServletResponse resp) {
        String newToken = generateToken();
        setCsrfToken(resp, newToken);
    }

    /** @deprecated Use {@link #rotateToken(HttpServletResponse)}. Kept for source compatibility. */
    @Deprecated
    public static void generateNewToken(HttpServletResponse resp) {
        rotateToken(resp);
    }

    private static String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        String nonce = Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
        String signature = computeHmac(nonce);
        return nonce + "." + signature;
    }

    static boolean verifyToken(String token) {
        if (token == null || !token.contains(".")) {
            return false;
        }
        int dotIndex = token.indexOf('.');
        String nonce = token.substring(0, dotIndex);
        String signature = token.substring(dotIndex + 1);
        String expectedSignature = computeHmac(nonce);
        return constantTimeEquals(signature, expectedSignature);
    }

    private static String computeHmac(String data) {
        try {
            String secret = FiberServer.get().getConfig().getJwtSecretKey();
            Mac mac = Mac.getInstance(HMAC_ALGO);
            mac.init(new SecretKeySpec(secret.getBytes(StandardCharsets.UTF_8), HMAC_ALGO));
            byte[] hmacBytes = mac.doFinal(data.getBytes(StandardCharsets.UTF_8));
            return Base64.getUrlEncoder().withoutPadding().encodeToString(hmacBytes);
        } catch (Exception e) {
            throw new RuntimeException("Failed to compute CSRF HMAC", e);
        }
    }

    private static boolean constantTimeEquals(String a, String b) {
        if (a == null || b == null) return false;
        byte[] aBytes = a.getBytes(StandardCharsets.UTF_8);
        byte[] bBytes = b.getBytes(StandardCharsets.UTF_8);
        return java.security.MessageDigest.isEqual(aBytes, bBytes);
    }

    private static String getCsrfTokenFromCookie(HttpServletRequest req) {
        jakarta.servlet.http.Cookie[] cookies = req.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie cookie : cookies) {
                if (CSRF_COOKIE.equals(cookie.getName())) {
                    return cookie.getValue();
                }
            }
        }
        return null;
    }

    private static void setCsrfToken(HttpServletResponse resp, String token) {
        boolean dev = safeIsDev();
        String cookieAttributes = "; Path=/; SameSite=Strict" + (!dev ? "; Secure" : "");

        resp.addHeader("Set-Cookie",
                CSRF_COOKIE + "=" + token + cookieAttributes);
        resp.setHeader(CSRF_HEADER, token);
    }
}
