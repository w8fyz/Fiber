package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.util.Map;

/**
 * Contract for OAuth2 identity providers (Discord, Google, ...).
 *
 * @param <T> The type of user entity used in the application
 */
public interface OAuth2Provider<T extends UserAuth> {

    /**
     * @return The provider's unique identifier, e.g. "discord".
     */
    String getProviderId();

    /**
     * @return The field name in {@code userInfo} that holds the provider's stable user id.
     */
    String getIdField();

    /**
     * Build the full authorization URL the browser should be redirected to.
     *
     * @param state       CSRF-protection state value
     * @param redirectUri Callback URL
     */
    String getAuthorizationUrl(String state, String redirectUri);

    /**
     * Exchange the authorization code, fetch the user profile and return both
     * the raw userInfo map and the token response (access + refresh + expiry)
     * so the caller can persist long-lived credentials.
     */
    OAuth2CallbackResult processCallback(String code, String redirectUri);

    /**
     * Use a stored {@code refresh_token} to obtain a new access token without
     * going through the authorization code flow again. Providers that do not
     * support refresh (rare) may return {@code null}; callers must then route
     * the user back through {@link #getAuthorizationUrl}.
     */
    default OAuth2TokenResponse refreshAccessToken(String refreshToken) {
        return null;
    }

    /**
     * Copy provider-specific fields from {@code userInfo} onto a newly created
     * application user.
     */
    void mapUserData(Map<String, Object> userInfo, T user);

    /**
     * Hook invoked after a successful callback when the caller wants provider
     * access to a just-issued access token. Default no-op.
     */
    default void useAccessToken(String accessToken, T user) {
    }
}
