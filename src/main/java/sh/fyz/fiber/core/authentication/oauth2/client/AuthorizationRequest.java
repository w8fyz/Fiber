package sh.fyz.fiber.core.authentication.oauth2.client;

public class AuthorizationRequest {
    private final String clientId;
    private final String redirectUri;
    private final String state;

    public AuthorizationRequest(String clientId, String redirectUri, String state) {
        this.clientId = clientId;
        this.redirectUri = redirectUri;
        this.state = state;
    }
}