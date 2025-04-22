package sh.fyz.fiber.core.authentication.oauth2;

public record OAuth2ApplicationInfo(String clientId, String clientSecret) {

    public String getBasicAuthHeader() {
        String credentials = clientId + ":" + clientSecret;
        return "Basic " + java.util.Base64.getEncoder().encodeToString(credentials.getBytes());
    }
} 