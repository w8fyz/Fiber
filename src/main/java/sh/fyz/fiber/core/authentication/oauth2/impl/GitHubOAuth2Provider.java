package sh.fyz.fiber.core.authentication.oauth2.impl;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.AbstractOAuth2Provider;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2TokenResponse;

import java.util.Map;

/**
 * GitHub OAuth2 provider implementation.
 * 
 * @param <T> The type of user entity used in the application
 */
public class GitHubOAuth2Provider<T extends UserAuth> extends AbstractOAuth2Provider<T> {
    private static final String AUTHORIZATION_ENDPOINT = "https://github.com/login/oauth/authorize";
    private static final String TOKEN_ENDPOINT = "https://github.com/login/oauth/access_token";
    private static final String USER_INFO_ENDPOINT = "https://api.github.com/user";
    private static final String DEFAULT_SCOPE = "user:email";

    /**
     * Creates a new GitHub OAuth2 provider.
     * 
     * @param clientId The GitHub application client ID
     * @param clientSecret The GitHub application client secret
     */
    public GitHubOAuth2Provider(String clientId, String clientSecret) {
        super(clientId, clientSecret, AUTHORIZATION_ENDPOINT, TOKEN_ENDPOINT, USER_INFO_ENDPOINT);
    }

    @Override
    public String getProviderId() {
        return "github";
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
        // This method should be implemented by subclasses to map GitHub-specific user data
        // to the application's user entity
        throw new UnsupportedOperationException(
            "This is a base class. Implement a subclass with your specific user type to map GitHub user data.");
    }
} 