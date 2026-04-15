package sh.fyz.fiber.middleware.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.middleware.Middleware;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class CsrfMiddleware implements Middleware {

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
            String headerToken = req.getHeader(CSRF_HEADER);
            String cookieToken = getCsrfTokenFromCookie(req);

            if (headerToken == null || cookieToken == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }

            if (!verifyToken(headerToken) || !headerToken.equals(cookieToken)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }

            generateNewToken(resp);
        } else {
            String existingToken = getCsrfTokenFromCookie(req);
            if (existingToken == null || !verifyToken(existingToken)) {
                generateNewToken(resp);
            }
        }

        return true;
    }

    public static void generateNewToken(HttpServletResponse resp) {
        String newToken = generateToken();
        setCsrfToken(resp, newToken);
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
        return MessageDigestEqual(signature, expectedSignature);
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

    private static boolean MessageDigestEqual(String a, String b) {
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
        String cookieAttributes = "; Path=/; SameSite=Strict" + (!FiberServer.get().isDev() ? "; Secure" : "");

        resp.addHeader("Set-Cookie",
                CSRF_COOKIE + "=" + token + cookieAttributes);
        resp.setHeader(CSRF_HEADER, token);
    }
}
