package sh.fyz.fiber.core.authentication.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.util.ResponseContext;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Simplified OAuth2 authentication service.
 *
 * @param <T> The type of user entity used in the application
 */
public abstract class OAuth2AuthenticationService<T extends UserAuth> {
    private static final long STATE_TTL_MINUTES = 10;
    private static final String STATE_COOKIE = "oauth_state";

    private final AuthenticationService<T> authenticationService;
    private final Map<String, OAuth2Provider<T>> providers;
    private final Map<String, StateEntry> stateStore;
    private final GenericRepository<T> userRepository;
    private final UserOAuth2TokenService tokenService;
    private final ScheduledExecutorService stateCleanupExecutor;

    public OAuth2AuthenticationService(AuthenticationService<T> authenticationService,
                                       GenericRepository<T> userRepository) {
        this(authenticationService, userRepository, null);
    }

    public OAuth2AuthenticationService(AuthenticationService<T> authenticationService,
                                       GenericRepository<T> userRepository,
                                       UserOAuth2TokenService tokenService) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.tokenService = tokenService;
        this.providers = new ConcurrentHashMap<>();
        this.stateStore = new ConcurrentHashMap<>();
        ScheduledExecutorService shared = null;
        try {
            shared = sh.fyz.fiber.FiberServer.get().getSharedExecutor();
        } catch (Exception e) {
            sh.fyz.fiber.core.log.FiberLog.handleSilent(e);
        }
        if (shared != null) {
            this.stateCleanupExecutor = shared;
        } else {
            this.stateCleanupExecutor = Executors.newSingleThreadScheduledExecutor(r -> {
                Thread t = Thread.ofVirtual().name("oauth2-state-cleanup-").unstarted(r);
                t.setDaemon(true);
                return t;
            });
        }
        this.stateCleanupExecutor.scheduleAtFixedRate(
                () -> {
                    long now = System.currentTimeMillis();
                    stateStore.entrySet().removeIf(e -> now > e.getValue().expiresAt);
                },
                1, 1, TimeUnit.MINUTES
        );
    }

    private static class StateEntry {
        final String providerId;
        final boolean browserBound;
        final long expiresAt;

        StateEntry(String providerId, boolean browserBound) {
            this.providerId = providerId;
            this.browserBound = browserBound;
            this.expiresAt = System.currentTimeMillis() + TimeUnit.MINUTES.toMillis(STATE_TTL_MINUTES);
        }
    }

    /**
     * Register an OAuth2 provider
     * @param provider The OAuth2 provider to register
     */
    public void registerProvider(OAuth2Provider<T> provider) {
        providers.put(provider.getProviderId(), provider);
    }

    /**
     * Get the OAuth2 provider by ID
     * @param providerId The provider ID
     * @return The OAuth2 provider
     */
    public OAuth2Provider<T> getProvider(String providerId) {
        return providers.get(providerId);
    }

    /**
     * Get the authorization URL for a specific provider.
     *
     * <p>The state is not bound to the browser that started the flow, so a callback URL
     * started by someone else is accepted (login CSRF). Prefer
     * {@link #getAuthorizationUrl(String, String, HttpServletResponse)}.</p>
     *
     * @param providerId The provider ID
     * @param redirectUri The callback URL
     * @return The authorization URL
     */
    public String getAuthorizationUrl(String providerId, String redirectUri) {
        return createAuthorizationUrl(providerId, redirectUri, null);
    }

    /**
     * Get the authorization URL for a specific provider, binding the state to the current
     * browser with a short-lived {@code oauth_state} cookie (HttpOnly, SameSite=Lax).
     * {@link #handleCallback} then rejects a callback whose request does not carry that cookie,
     * so an attacker cannot log a victim into the attacker's account by sending them a callback
     * URL. The callback request must reach this server with the browser's cookies: a top-level
     * GET redirect works, a cross-site POST ({@code response_mode=form_post}) does not carry the
     * SameSite=Lax cookie.
     *
     * @param providerId The provider ID
     * @param redirectUri The callback URL
     * @param response The current response, to set the state cookie on
     * @return The authorization URL
     */
    public String getAuthorizationUrl(String providerId, String redirectUri, HttpServletResponse response) {
        return createAuthorizationUrl(providerId, redirectUri, response);
    }

    private String createAuthorizationUrl(String providerId, String redirectUri, HttpServletResponse response) {
        OAuth2Provider<T> provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }

        String state = UUID.randomUUID().toString();
        stateStore.put(state, new StateEntry(providerId, response != null));
        if (response != null) {
            response.addHeader("Set-Cookie", stateCookie(state, TimeUnit.MINUTES.toSeconds(STATE_TTL_MINUTES)));
        }

        return provider.getAuthorizationUrl(state, redirectUri);
    }

    private String stateCookie(String value, long maxAgeSeconds) {
        // Lax, not the auth cookies' Strict default: the callback is a cross-site navigation from the provider.
        return STATE_COOKIE + "=" + value + "; Path=/; Max-Age=" + maxAgeSeconds + "; HttpOnly; SameSite=Lax"
                + (authenticationService.getCookieConfig().isSecure() ? "; Secure" : "");
    }

    private static String readCookie(HttpServletRequest request, String name) {
        jakarta.servlet.http.Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (jakarta.servlet.http.Cookie c : cookies) {
                if (name.equals(c.getName())) {
                    return c.getValue();
                }
            }
        }
        return null;
    }

    public String getProviderIdFromState(String state) {
        StateEntry entry = stateStore.remove(state);
        if (entry == null || System.currentTimeMillis() > entry.expiresAt) {
            return null;
        }
        return entry.providerId;
    }

    public Map<String, OAuth2Provider<T>> getProviders() {
        return providers;
    }

    public UserOAuth2TokenService getTokenService() {
        return tokenService;
    }

    /**
     * Handle the OAuth2 callback.
     *
     * <p>Short-circuits the provider round-trip when the incoming request
     * already carries valid Fiber credentials (cookie or bearer). That is the
     * common case for repeat logins / reconnects and avoids a new
     * {@code /oauth2/token} POST against the provider — the root cause of
     * provider-side rate limiting on busy apps.
     */
    public ResponseContext<T> handleCallback(String code, String state, String redirectUri,
                                             HttpServletRequest request, HttpServletResponse response) {
        T existing = authenticationService.resolveFromRequest(request);
        if (existing != null) {
            stateStore.remove(state);
            return new ResponseContext<>(existing, null, null);
        }

        StateEntry stateEntry = stateStore.remove(state);
        if (stateEntry == null || System.currentTimeMillis() > stateEntry.expiresAt) {
            throw new IllegalArgumentException("Invalid or expired state parameter");
        }
        if (stateEntry.browserBound) {
            String cookieState = readCookie(request, STATE_COOKIE);
            if (cookieState == null || !MessageDigest.isEqual(
                    cookieState.getBytes(StandardCharsets.UTF_8), state.getBytes(StandardCharsets.UTF_8))) {
                // The cookie is left alone: it may belong to a newer flow started in another tab.
                throw new IllegalArgumentException("State was not issued to this browser");
            }
            response.addHeader("Set-Cookie", stateCookie("", 0));
        }
        String providerId = stateEntry.providerId;
        if (providerId == null) {
            throw new IllegalArgumentException("Invalid state parameter");
        }

        OAuth2Provider<T> provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }
        OAuth2CallbackResult result = provider.processCallback(code, redirectUri);

        ResponseContext<T> user = findOrCreateUser(result.userInfo(), provider);
        if (user.getState() == null) {
            if (tokenService != null && user.getResult() != null) {
                tokenService.saveOrUpdate(user.getResult().getId(), provider.getProviderId(), result.tokens());
            }
            authenticationService.setAuthCookies(user.getResult(), request, response);
        }
        return user;
    }

    /**
     * Convenience for app code that needs to call the provider's API after
     * initial login. Returns the stored access token (refreshing if needed)
     * without re-running the authorization code flow.
     *
     * @return a valid access token, or {@code null} if no row or refresh failed.
     */
    public String getValidAccessToken(T user, String providerId) {
        if (tokenService == null || user == null || providerId == null) {
            return null;
        }
        OAuth2Provider<T> provider = providers.get(providerId);
        if (provider == null) {
            return null;
        }
        return tokenService.getValidAccessToken(user.getId(), provider);
    }

    /**
     * Find or create a user based on OAuth2 user info
     * @param userInfo The user info from the OAuth2 provider
     * @param provider The OAuth2 provider
     * @return The user
     */
    protected abstract ResponseContext<T> findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<T> provider);
}
