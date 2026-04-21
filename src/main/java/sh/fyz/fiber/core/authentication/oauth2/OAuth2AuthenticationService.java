package sh.fyz.fiber.core.authentication.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.util.ResponseContext;

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
        } catch (Exception ignored) {
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
        final long expiresAt;

        StateEntry(String providerId) {
            this.providerId = providerId;
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
     * Get the authorization URL for a specific provider
     * @param providerId The provider ID
     * @param redirectUri The callback URL
     * @return The authorization URL
     */
    public String getAuthorizationUrl(String providerId, String redirectUri) {
        OAuth2Provider<T> provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }

        String state = UUID.randomUUID().toString();
        stateStore.put(state, new StateEntry(providerId));

        return provider.getAuthorizationUrl(state, redirectUri);
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
