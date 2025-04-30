package sh.fyz.fiber.core.authentication.oauth2;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.architect.repositories.GenericRepository;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Simplified OAuth2 authentication service.
 * 
 * @param <T> The type of user entity used in the application
 */
public abstract class OAuth2AuthenticationService<T extends UserAuth> {
    private final AuthenticationService<T> authenticationService;
    private final Map<String, OAuth2Provider<T>> providers;
    private final Map<String, String> stateStore;
    public final GenericRepository<T> userRepository;

    public OAuth2AuthenticationService(AuthenticationService<T> authenticationService, GenericRepository<T> userRepository) {
        this.authenticationService = authenticationService;
        this.userRepository = userRepository;
        this.providers = new ConcurrentHashMap<>();
        this.stateStore = new ConcurrentHashMap<>();
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
        stateStore.put(state, providerId);
        
        return provider.getAuthorizationUrl(state, redirectUri);
    }

    /**
     * Handle the OAuth2 callback
     * @param code The authorization code
     * @param state The state parameter
     * @param redirectUri The callback URL
     * @param request The HTTP request
     * @param response The HTTP response
     * @return The authenticated user
     */
    public T handleCallback(String code, String state, String redirectUri, 
                          HttpServletRequest request, HttpServletResponse response) {
        String providerId = stateStore.remove(state);
        if (providerId == null) {
            throw new IllegalArgumentException("Invalid state parameter");
        }

        OAuth2Provider<T> provider = providers.get(providerId);
        if (provider == null) {
            throw new IllegalArgumentException("Provider not found: " + providerId);
        }
        System.out.println("====== OAuth2 Callback ====");
        // Process the callback and get user info
        Map<String, Object> userInfo = provider.processCallback(code, redirectUri);
        System.out.println("User Info: ");
        for (Map.Entry<String, Object> entry : userInfo.entrySet()) {
            System.out.println(entry.getKey() + ": " + entry.getValue());
        }
        System.out.println("====");
        // Find or create user
        T user = findOrCreateUser(userInfo, provider);

        System.out.println("User: " + user.getId());
        
        // Set authentication cookies
        authenticationService.setAuthCookies(user, request, response);
        System.out.println("====== OAuth2 Callback ====");
        return user;
    }

    /**
     * Find or create a user based on OAuth2 user info
     * @param userInfo The user info from the OAuth2 provider
     * @param provider The OAuth2 provider
     * @return The user
     */
    protected abstract T findOrCreateUser(Map<String, Object> userInfo, OAuth2Provider<T> provider);
} 