package sh.fyz.fiber.core.authentication;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionContext;
import sh.fyz.fiber.core.session.SessionService;
import sh.fyz.fiber.middleware.impl.CsrfMiddleware;
import sh.fyz.fiber.util.HttpUtil;

import java.time.Duration;
import java.util.Map;

public abstract class AuthenticationService<T extends UserAuth> {

    private final GenericRepository<T> userRepository;
    private String refreshTokenPath = "/auth";
    private final AuthCookieConfig cookieConfig;

    private final Cache<Object, T> userCache;

    public AuthenticationService(GenericRepository<T> userRepository, String authEndpoint) {
        this.userRepository = userRepository;
        this.refreshTokenPath = authEndpoint;
        this.cookieConfig = new AuthCookieConfig()
                .setSameSite(FiberServer.get().isDev() ? SameSitePolicy.LAX : SameSitePolicy.STRICT)
                .setSecure(!FiberServer.get().isDev());
        this.userCache = buildDefaultCache();
    }

    public AuthenticationService(GenericRepository<T> userRepository, String authEndpoint, AuthCookieConfig cookieConfig) {
        this.userRepository = userRepository;
        this.refreshTokenPath = authEndpoint;
        this.cookieConfig = cookieConfig;
        this.userCache = buildDefaultCache();
    }

    private Cache<Object, T> buildDefaultCache() {
        return Caffeine.newBuilder()
                .maximumSize(10_000)
                .expireAfterWrite(Duration.ofSeconds(30))
                .build();
    }

    /**
     * Override to customize the user cache (TTL, size, etc.).
     * Return null to disable caching entirely.
     */
    protected Cache<Object, T> buildUserCache() {
        return buildDefaultCache();
    }

    public Class<T> getUserClass() {
        return userRepository.getEntityClass();
    }

    public T getUserById(Object id) {
        if (userCache == null) {
            return userRepository.findById(id);
        }
        return userCache.get(id, k -> userRepository.findById(k));
    }

    /**
     * Evict a user from cache. Call this after any user mutation (save, update, delete).
     */
    public void evictUser(Object id) {
        if (userCache != null) {
            userCache.invalidate(id);
        }
    }

    /** Evict all cached users. */
    public void evictAllUsers() {
        if (userCache != null) {
            userCache.invalidateAll();
        }
    }

    public boolean validateCredentials(UserAuth user, String password) {
        return UserFieldUtil.validatePassword(user, password);
    }

    /**
     * Override this method to provide an efficient database query for user lookup by identifier.
     * The default implementation loads all users in memory (not suitable for production).
     */
    public UserAuth findUserByIdentifer(String identifier) {
        return findByIdentifier(identifier);
    }

    protected UserAuth findByIdentifier(String identifier) {
        return UserFieldUtil.findUserByIdentifier(identifier, userRepository.all());
    }

    public boolean doesIdentifiersAlreadyExists(UserAuth user) {
        Map<String, String> identifiers = UserFieldUtil.getIdentifiers(user);
        for (Map.Entry<String, String> entry : identifiers.entrySet()) {
            if (findUserByIdentifer(entry.getValue()) != null) {
                return true;
            }
        }
        return false;
    }

    public String generateToken(UserAuth user, HttpServletRequest request) {
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return JwtUtil.generateToken(user, ipAddress, userAgent);
    }

    public boolean validateToken(String token, HttpServletRequest request) {
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");
        return JwtUtil.validateToken(token, ipAddress, userAgent) != null;
    }

    public void setAuthCookies(UserAuth user, HttpServletRequest request, HttpServletResponse response) {
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        String sessionId = null;
        SessionService sessionService = FiberServer.get().getSessionService();
        if (sessionService != null) {
            FiberSession session = sessionService.createSession(user, request);
            sessionId = session.getSessionId();
            SessionContext.set(session);
        }

        String accessToken = JwtUtil.generateToken(user, ipAddress, userAgent, sessionId);
        String refreshToken = JwtUtil.generateRefreshToken(user, ipAddress, userAgent, sessionId);

        response.addHeader("Set-Cookie",
            "access_token=" + accessToken + cookieConfig.buildCookieAttributesWithMaxAge(request.getHeader("Origin"), cookieConfig.getAccessTokenMaxAge()));

        AuthCookieConfig refreshCookieConfig = new AuthCookieConfig()
                .setSameSite(cookieConfig.getSameSite())
                .setSecure(cookieConfig.isSecure())
                .setHttpOnly(cookieConfig.isHttpOnly())
                .setDomains(cookieConfig.getDomains())
                .setPath(refreshTokenPath);

        response.addHeader("Set-Cookie",
            "refresh_token=" + refreshToken + refreshCookieConfig.buildCookieAttributesWithMaxAge(request.getHeader("Origin"), cookieConfig.getRefreshTokenMaxAge()));

        // Rotate CSRF token after a successful authentication change to prevent
        // session-fixation style CSRF attacks.
        try {
            if (FiberServer.get().getCsrfMiddleware() != null) {
                CsrfMiddleware.rotateToken(response);
            }
        } catch (Exception ignored) {
            // CSRF not configured — nothing to rotate.
        }
    }

    public void clearAuthCookies(HttpServletRequest request, HttpServletResponse response) {
        // Revoke the refresh token so it cannot be replayed even if it was leaked
        // (network capture, malicious browser extension, etc.).
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if ("refresh_token".equals(c.getName())) {
                    JwtUtil.revokeRefreshToken(c.getValue());
                }
            }
        }

        SessionService sessionService = FiberServer.get().getSessionService();
        if (sessionService != null) {
            FiberSession currentSession = SessionContext.current();
            if (currentSession != null) {
                sessionService.invalidate(currentSession.getSessionId());
            }
        }

        response.addHeader("Set-Cookie",
            "access_token=" + cookieConfig.buildCookieAttributesWithMaxAge(request.getHeader("Origin"), 0));

        AuthCookieConfig refreshCookieConfig = new AuthCookieConfig()
                .setSameSite(cookieConfig.getSameSite())
                .setSecure(cookieConfig.isSecure())
                .setHttpOnly(cookieConfig.isHttpOnly())
                .setDomains(cookieConfig.getDomains())
                .setPath(refreshTokenPath);

        response.addHeader("Set-Cookie",
            "refresh_token=" + refreshCookieConfig.buildCookieAttributesWithMaxAge(request.getHeader("Origin"), 0));
    }
}
