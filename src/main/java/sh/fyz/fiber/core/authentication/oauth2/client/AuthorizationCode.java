package sh.fyz.fiber.core.authentication.oauth2.client;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

public class AuthorizationCode {
    private final String code;
    private final UserAuth user;
    private final String clientId;
    private final long expiresAt;

    public AuthorizationCode(String code, UserAuth user, String clientId) {
        this.code = code;
        this.user = user;
        this.clientId = clientId;
        this.expiresAt = System.currentTimeMillis() + 600000; // 10 minutes
    }

    public String getCode() {
        return code;
    }

    public UserAuth getUser() {
        return user;
    }

    public String getClientId() {
        return clientId;
    }

    public boolean isExpired() {
        return System.currentTimeMillis() > expiresAt;
    }
}
