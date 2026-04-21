package sh.fyz.fiber.core.authentication.oauth2.impl;

import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.AbstractOAuth2Provider;

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

    private final boolean forceConsent;

    public DiscordOAuth2Provider(String clientId, String clientSecret, String scopes) {
        this(clientId, clientSecret, scopes, false);
    }

    public DiscordOAuth2Provider(String clientId, String clientSecret) {
        this(clientId, clientSecret, DEFAULT_SCOPE, false);
    }

    /**
     * @param forceConsent when {@code true}, adds {@code prompt=consent} to the
     *     authorization URL so Discord always re-displays the consent screen.
     *     Default {@code false} — lets Discord auto-approve returning users,
     *     which also reduces UX friction. Does NOT reduce {@code /oauth2/token}
     *     calls; the code exchange happens either way.
     */
    public DiscordOAuth2Provider(String clientId, String clientSecret, String scopes, boolean forceConsent) {
        super(clientId, clientSecret, AUTHORIZATION_ENDPOINT, TOKEN_ENDPOINT, USER_INFO_ENDPOINT, scopes);
        this.forceConsent = forceConsent;
    }

    @Override
    public String getProviderId() {
        return "discord";
    }

    @Override
    public String getIdField() {
        return "id";
    }

    @Override
    public String getAuthorizationUrl(String state, String redirectUri) {
        return buildAuthorizationUrl(state, redirectUri, defaultScope);
    }

    @Override
    protected void customizeAuthorizationParams(Map<String, String> params) {
        if (forceConsent) {
            params.put("prompt", "consent");
        }
    }

    @Override
    public void mapUserData(Map<String, Object> userInfo, T user) {
        throw new UnsupportedOperationException(
            "This is a base class. Implement a subclass with your specific user type to map Discord user data.");
    }
}
