package sh.fyz.fiber.core.authentication.oauth2;

import jakarta.servlet.http.HttpServletRequest;

public interface OAuth2ApplicationAuthenticator {
    OAuth2ApplicationInfo authenticate(HttpServletRequest request);
} 