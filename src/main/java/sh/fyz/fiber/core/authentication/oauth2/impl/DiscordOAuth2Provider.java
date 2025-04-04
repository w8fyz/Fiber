package sh.fyz.fiber.core.authentication.oauth2.impl;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.AbstractOAuth2Provider;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2TokenResponse;

import java.util.Map;

/**
 * Discord OAuth2 provider implementation.
 * This provider allows users to authenticate using their Discord accounts.
 * 
 * @param <T> The type of user entity used in the application
 */
public class DiscordOAuth2Provider<T extends UserAuth> extends AbstractOAuth2Provider<T> {
    private static final String AUTHORIZATION_ENDPOINT = "https://discord.com/api/oauth2/authorize";
    private static final String TOKEN_ENDPOINT = "https://discord.com/api/oauth2/token";
    private static final String USER_INFO_ENDPOINT = "https://discord.com/api/users/@me";
    private static final String DEFAULT_SCOPE = "identify email";

    /**
     * Creates a new Discord OAuth2 provider.
     * 
     * @param clientId The Discord application client ID
     * @param clientSecret The Discord application client secret
     */
    public DiscordOAuth2Provider(String clientId, String clientSecret) {
        super(clientId, clientSecret, AUTHORIZATION_ENDPOINT, TOKEN_ENDPOINT, USER_INFO_ENDPOINT);
    }

    @Override
    public String getProviderId() {
        return "discord";
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
    public Map<String, Object> getUserInfo(String accessToken) {
        return getUserInfoFromToken(accessToken);
    }
    
    @Override
    public void mapUserData(Map<String, Object> userInfo, T user) {
        // This method should be implemented by subclasses to map Discord-specific user data
        // to the application's user entity
        throw new UnsupportedOperationException(
            "This is a base class. Implement a subclass with your specific user type to map Discord user data.");
    }
} 