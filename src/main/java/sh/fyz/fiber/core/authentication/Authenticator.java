package sh.fyz.fiber.core.authentication;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

public interface Authenticator {
    AuthScheme scheme();
    UserAuth authenticate(HttpServletRequest request);
} 