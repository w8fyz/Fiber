package sh.fyz.fiber.middleware.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.middleware.Middleware;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

public class CsrfMiddleware implements Middleware {

    private static final String CSRF_HEADER = "X-CSRF-TOKEN";
    private static final String CSRF_COOKIE = "XSRF-TOKEN";
    private static final SecureRandom secureRandom = new SecureRandom();
    private static final Set<String> SAFE_METHODS = new HashSet<>(Set.of("GET", "HEAD", "OPTIONS"));

    @Override
    public int priority() {
        return 1;
    }

    @Override
    public boolean handle(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String method = req.getMethod();

        // Pour les méthodes non-sécurisées, vérifier le token
        if (!SAFE_METHODS.contains(method)) {
            String headerToken = req.getHeader(CSRF_HEADER);
            String cookieToken = getCsrfTokenFromCookie(req);

            if (headerToken == null || cookieToken == null || !headerToken.equals(cookieToken)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Invalid CSRF token");
                return false;
            }
        }

        generateNewToken(resp);

        return true;
    }
    public static void generateNewToken(HttpServletResponse resp) {
        String newToken = generateToken();
        setCsrfToken(resp, newToken);
    }

    private static String generateToken() {
        byte[] randomBytes = new byte[32];
        secureRandom.nextBytes(randomBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(randomBytes);
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
