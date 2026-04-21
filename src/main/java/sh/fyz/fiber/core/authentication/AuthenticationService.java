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
        writeAuthCookies(user, request, response, null);
    }

    /**
     * Rotate access + refresh cookies while preserving the current server-side
     * session. The caller passes the refresh token still held by the client;
     * the session id embedded in that token is reused so the DB row tied to
     * the user's "device" is kept intact. Use this instead of
     * {@link #setAuthCookies(UserAuth, HttpServletRequest, HttpServletResponse)}
     * inside refresh endpoints — otherwise every refresh spawns a fresh session
     * and {@code getUserSessions()} accumulates duplicates.
     *
     * <p>Behavior:
     * <ul>
     *   <li>Validates the refresh token (signature, TTL, IP/UA binding, not revoked).</li>
     *   <li>If a {@link SessionService} is configured and the refresh token carries
     *       a session id, the session must still exist and be active — otherwise
     *       {@code null} is returned (the caller should clear cookies and reply 401).
     *       This is the correct behavior when the user revoked the session from
     *       another device.</li>
     *   <li>Touches the session's {@code lastAccessedAt}.</li>
     *   <li>Revokes the old refresh token to prevent replay attacks.</li>
     *   <li>Issues a new access + refresh token tied to the <em>same</em> session id.</li>
     *   <li>Rotates the CSRF token.</li>
     * </ul>
     *
     * @return the refreshed {@link UserAuth} on success, {@code null} on any failure.
     */
    @SuppressWarnings("unchecked")
    public T refreshAuthCookies(String refreshToken, HttpServletRequest request, HttpServletResponse response) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        if (!JwtUtil.validateRefreshToken(refreshToken, ipAddress, userAgent)) {
            return null;
        }
        Object userId = JwtUtil.extractId(refreshToken);
        if (userId == null) {
            return null;
        }
        T user = getUserById(userId);
        if (user == null) {
            return null;
        }

        String existingSessionId = JwtUtil.extractSessionId(refreshToken);
        String sessionIdToUse = existingSessionId;
        SessionService sessionService = FiberServer.get().getSessionService();
        if (sessionService != null) {
            if (existingSessionId != null) {
                FiberSession session = sessionService.getSession(existingSessionId);
                if (session == null) {
                    // Session revoked server-side (logout from another device,
                    // admin action, cleanup). Fail the refresh instead of
                    // silently issuing a new session — that would defeat the
                    // whole point of revocation.
                    return null;
                }
                sessionService.touchSession(existingSessionId);
                SessionContext.set(session);
            } else {
                // Legacy refresh token minted before server-side sessions
                // were enabled. Bootstrap a session now so future refreshes
                // can be tracked.
                FiberSession session = sessionService.createSession(user, request);
                sessionIdToUse = session.getSessionId();
                SessionContext.set(session);
            }
        }

        // Invalidate the token we just consumed before minting its replacement.
        // If the client never receives the new cookie (network drop between
        // Set-Cookie and Response delivery) they'll need to log in again — the
        // trade-off is worth it: a leaked refresh token cannot be replayed.
        JwtUtil.revokeRefreshToken(refreshToken);

        writeAuthCookies(user, request, response, sessionIdToUse);
        return user;
    }

    /**
     * Shared cookie-writing path used by both {@link #setAuthCookies} (fresh
     * login) and {@link #refreshAuthCookies} (rotation). When
     * {@code preservedSessionId} is {@code null}, a new session is created
     * (login flow). When it is non-null, the provided id is reused as-is
     * (refresh flow).
     */
    private void writeAuthCookies(UserAuth user, HttpServletRequest request, HttpServletResponse response,
                                  String preservedSessionId) {
        String ipAddress = HttpUtil.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        String sessionId = preservedSessionId;
        if (sessionId == null) {
            SessionService sessionService = FiberServer.get().getSessionService();
            if (sessionService != null) {
                FiberSession session = sessionService.createSession(user, request);
                sessionId = session.getSessionId();
                SessionContext.set(session);
            }
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
