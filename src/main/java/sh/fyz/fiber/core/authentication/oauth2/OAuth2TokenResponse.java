package sh.fyz.fiber.core.authentication.oauth2;

/**
 * Class representing an OAuth2 token response.
 */
public class OAuth2TokenResponse {
    private final String accessToken;
    private final String tokenType;
    private final Long expiresIn;
    private final String refreshToken;
    private final String scope;

    public OAuth2TokenResponse(String accessToken, String tokenType, Long expiresIn, String refreshToken, String scope) {
        this.accessToken = accessToken;
        this.tokenType = tokenType;
        this.expiresIn = expiresIn;
        this.refreshToken = refreshToken;
        this.scope = scope;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }

    public Long getExpiresIn() {
        return expiresIn;
    }

    public String getRefreshToken() {
        return refreshToken;
    }

    public String getScope() {
        return scope;
    }
} 