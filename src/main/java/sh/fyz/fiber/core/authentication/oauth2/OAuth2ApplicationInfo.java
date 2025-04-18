package sh.fyz.fiber.core.authentication.oauth2;

public class OAuth2ApplicationInfo {
    private final String clientId;
    private final String clientSecret;

    public OAuth2ApplicationInfo(String clientId, String clientSecret) {
        this.clientId = clientId;
        this.clientSecret = clientSecret;
    }

    public String getClientId() {
        return clientId;
    }

    public String getClientSecret() {
        return clientSecret;
    }

    public String getBasicAuthHeader() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
} 