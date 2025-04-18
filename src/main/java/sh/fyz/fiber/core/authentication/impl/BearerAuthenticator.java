package sh.fyz.fiber.core.authentication.impl;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.Authenticator;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.JwtUtil;

public class BearerAuthenticator implements Authenticator {
    @Override
    public AuthScheme scheme() {
        return AuthScheme.BEARER;
    }

    @Override
    public UserAuth authenticate(HttpServletRequest request) {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7);
            if (JwtUtil.validateToken(token, request.getRemoteAddr(), request.getHeader("User-Agent"))) {
                return FiberServer.get().getAuthService().getUserById(AuthMiddleware.getCurrentUserId(request));
            }
        }
        return null;
    }
} 