package sh.fyz.fiber.core.authentication.oauth2.impl;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.AbstractOAuth2Provider;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2TokenResponse;

import java.util.Map;

/**
 * Google OAuth2 provider implementation.
 * 
 * @param <T> The type of user entity used in the application
 */
public class GoogleOAuth2Provider<T extends UserAuth> extends AbstractOAuth2Provider<T> {
    private static final String AUTHORIZATION_ENDPOINT = "https://accounts.google.com/o/oauth2/v2/auth";
    private static final String TOKEN_ENDPOINT = "https://oauth2.googleapis.com/token";
    private static final String USER_INFO_ENDPOINT = "https://www.googleapis.com/oauth2/v2/userinfo";
    private static final String DEFAULT_SCOPE = "email profile";

    /**
     * Creates a new Google OAuth2 provider.
     * 
     * @param clientId The Google application client ID
     * @param clientSecret The Google application client secret
     */
    public GoogleOAuth2Provider(String clientId, String clientSecret) {
        super(clientId, clientSecret, AUTHORIZATION_ENDPOINT, TOKEN_ENDPOINT, USER_INFO_ENDPOINT);
    }

    @Override
    public String getProviderId() {
        return "google";
    }

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        return buildAuthorizationUrl(state, redirectUri, DEFAULT_SCOPE);
    }

    @Override
    public OAuth2TokenResponse getAccessToken(String code, String redirectUri) {
        return exchangeCodeForToken(code, redirectUri);
    }

    @Override
    public Map<String, String> getUserInfo(String accessToken) {
        return getUserInfoFromToken(accessToken);
    }
    
    @Override
    public void mapUserData(Map<String, Object> userInfo, T user) {
        // This method should be implemented by subclasses to map Google-specific user data
        // to the application's user entity
        throw new UnsupportedOperationException(
            "This is a base class. Implement a subclass with your specific user type to map Google user data.");
    }
} 