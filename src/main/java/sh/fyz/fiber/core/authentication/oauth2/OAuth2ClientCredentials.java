package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.OAuth2Client;

/**
 * Returned once at client registration. The {@code clientSecretPlaintext} value is the
 * <b>only</b> opportunity for the caller to obtain the raw secret — the persisted
 * {@link OAuth2Client} only stores the BCrypt hash.
 */
public record OAuth2ClientCredentials(OAuth2Client client, String clientSecretPlaintext) {
    public String clientId() {
        return client.getClientId();
    }
}
