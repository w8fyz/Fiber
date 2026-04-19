package sh.fyz.fiber.core.security.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.FiberServer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * CORS service with safe handling of credentialed requests.
 *
 * <p>Reflecting the request {@code Origin} header is only performed when the origin
 * matches an <b>exact</b> entry of the allow-list. Wildcard-matching origins are
 * rejected when {@code Access-Control-Allow-Credentials: true} would be required,
 * unless development mode is active.</p>
 *
 * <p>The {@code Origin: null} header (file://, sandboxed iframes, etc.) is only honored
 * in dev mode; in production, it is always rejected.</p>
 */
public class CorsService {

    private static final Logger logger = LoggerFactory.getLogger(CorsService.class);

    private List<String> allowedOrigins = new ArrayList<>();
    private boolean allowNullOrigin = false;
    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = Arrays.asList("Content-Type", "Authorization");
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public CorsService() {
        // Par défaut, n'accepte aucune origine
    }

    /**
     * Allow the special "null" origin. <b>Only honored in development mode</b> — calls in
     * production are ignored with a warning.
     */
    public CorsService allowNullOrigin() {
        this.allowNullOrigin = true;
        return this;
    }

    public CorsService addAllowedOrigin(String origin) {
        this.allowedOrigins.add(origin);
        return this;
    }

    public CorsService setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = new ArrayList<>(origins);
        return this;
    }

    public CorsService setAllowedMethods(List<String> methods) {
        this.allowedMethods = new ArrayList<>(methods);
        return this;
    }

    public CorsService setAllowedHeaders(List<String> headers) {
        this.allowedHeaders = new ArrayList<>(headers);
        return this;
    }

    public CorsService setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
        return this;
    }

    public CorsService setMaxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    private boolean matchesWildcard(String pattern, String origin) {
        int pi = 0, oi = 0;
        int pLen = pattern.length(), oLen = origin.length();
        int starIdx = -1, matchIdx = 0;

        while (oi < oLen) {
            if (pi < pLen && (pattern.charAt(pi) == origin.charAt(oi))) {
                pi++;
                oi++;
            } else if (pi < pLen && pattern.charAt(pi) == '*') {
                starIdx = pi;
                matchIdx = oi;
                pi++;
            } else if (starIdx != -1) {
                pi = starIdx + 1;
                matchIdx++;
                oi = matchIdx;
            } else {
                return false;
            }
        }

        while (pi < pLen && pattern.charAt(pi) == '*') {
            pi++;
        }
        return pi == pLen;
    }

    public boolean isOriginAllowed(String origin) {
        if (origin == null) {
            if (allowNullOrigin && isDev()) return true;
            if (allowNullOrigin) {
                logger.warn("[CORS] Ignoring allowNullOrigin in production mode");
            }
            return false;
        }
        if (allowedOrigins == null || allowedOrigins.isEmpty()) {
            return false;
        }

        for (String allowed : allowedOrigins) {
            if ("*".equals(allowed)) {
                return true;
            }
            if (!allowed.contains("*") && allowed.equals(origin)) {
                return true;
            }
            if (allowed.contains("*") && matchesWildcard(allowed, origin)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Returns true iff {@code origin} appears as an <b>exact</b> entry (no wildcard) in
     * {@code allowedOrigins}. Used to decide whether reflecting the origin header with
     * credentials is safe.
     */
    private boolean isExactOrigin(String origin) {
        if (origin == null) return false;
        for (String allowed : allowedOrigins) {
            if (!allowed.contains("*") && allowed.equals(origin)) return true;
        }
        return false;
    }

    public void handlePreflightRequest(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        String requestMethod = request.getHeader("Access-Control-Request-Method");

        if ((origin != null || isDev()) && requestMethod != null && isOriginAllowed(origin)) {
            configureCorsHeaders(request, response);
            response.setStatus(HttpServletResponse.SC_OK);
        } else {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }

    public void configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if (!isOriginAllowed(origin)) {
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            return;
        }

        boolean dev = isDev();

        if (allowCredentials) {
            // Reflecting the request Origin with credentials is only safe when the origin
            // is an exact (non-wildcard) match. Wildcard matches are refused in production.
            if (origin == null) {
                // null origin only in dev — no Access-Control-Allow-Origin header.
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            if (!isExactOrigin(origin) && !dev) {
                logger.warn("[CORS] Refusing to reflect wildcard-matched origin '{}' with credentials in production", origin);
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                return;
            }
            response.setHeader("Access-Control-Allow-Origin", origin);
            response.setHeader("Access-Control-Allow-Credentials", "true");
        } else {
            response.setHeader("Access-Control-Allow-Origin",
                    allowedOrigins.contains("*") ? "*" : origin);
        }

        response.setHeader("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
        response.setHeader("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
        response.setHeader("Access-Control-Max-Age", String.valueOf(maxAge));
        response.addHeader("Vary", "Origin");
    }

    private static boolean isDev() {
        try {
            return FiberServer.get().isDev();
        } catch (Exception e) {
            return false;
        }
    }
}
