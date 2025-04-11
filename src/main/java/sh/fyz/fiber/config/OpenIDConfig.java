package sh.fyz.fiber.config;

public class OpenIDConfig {
    private final String clientId;
    private final String clientSecret;
    private final String redirectUri;
    private final String issuerUri;
    private final String scope;

    public OpenIDConfig(String clientId, String clientSecret, String redirectUri, String issuerUri) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.redirectUri = redirectUri;
        this.issuerUri = issuerUri;
        this.scope = "openid profile email";
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getRedirectUri() {
        return redirectUri;
    }

    public String getIssuerUri() {
        return issuerUri;
    }

    public String getScope() {
        return scope;
    }
} 