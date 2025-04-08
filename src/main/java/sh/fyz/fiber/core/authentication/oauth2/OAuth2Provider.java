package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

/**
 * Interface defining the contract for OAuth2 providers.
 * Implement this interface to create custom OAuth2 providers or extend existing ones.
 * 
 * @param <T> The type of user entity used in the application
 */
public interface OAuth2Provider<T extends UserAuth> {
    /**
     * Get the provider's unique identifier
     * @return The provider ID
     */
    String getProviderId();

    /**
     * Get the authorization URL for the OAuth2 flow
     * @param state A random state parameter for security
     * @param redirectUri The callback URL after authorization
     * @return The authorization URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Exchange the authorization code for an access token
     * @param code The authorization code
     * @param redirectUri The callback URL used in the authorization request
     * @return The access token response
     */
    OAuth2TokenResponse getAccessToken(String code, String redirectUri);

    /**
     * Get user information using the access token
     * @param accessToken The OAuth2 access token
     * @return Map containing user information
     */
    Map<String, String> getUserInfo(String accessToken);

    /**
     * Get the provider's client ID
     * @return The client ID
     */
    String getClientId();

    /**
     * Get the provider's client secret
     * @return The client secret
     */
    String getClientSecret();
    
    /**
     * Map provider-specific user data to the application's user entity
     * @param userInfo The user information from the provider
     * @param user The user entity to map data to
     */
    void mapUserData(Map<String, Object> userInfo, T user);
} 