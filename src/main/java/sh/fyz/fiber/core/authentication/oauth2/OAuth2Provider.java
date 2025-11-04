package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

/**
 * Simplified interface defining the contract for OAuth2 providers.
 * 
 * @param <T> The type of user entity used in the application
 */
public interface OAuth2Provider<T extends UserAuth> {
    /**
     * Get the provider's unique identifier
     * @return The provider ID
     */
    String getProviderId();

    String getIdField();

    /**
     * Get the authorization URL for the OAuth2 flow
     * @param state A random state parameter for security
     * @param redirectUri The callback URL after authorization
     * @return The authorization URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Process the OAuth2 callback and return user information
     * @param code The authorization code
     * @param redirectUri The callback URL used in the authorization request
     * @return Map containing user information
     */
    Map<String, Object> processCallback(String code, String redirectUri);
    
    /**
     * Map provider-specific user data to the application's user entity
     * @param userInfo The user information from the provider
     * @param user The user entity to map data to
     */
    void mapUserData(Map<String, Object> userInfo, T user);
    void useAccessToken(String accessToken, T user);
} 