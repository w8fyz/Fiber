package sh.fyz.fiber.core.authentication.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
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
            Claims claims = JwtUtil.validateToken(token, request.getRemoteAddr(), request.getHeader("User-Agent"));
            if (claims != null) {
                if (!SessionValidator.validate(claims)) {
                    return null;
                }

                Object userId = claims.get("id");
                request.setAttribute("userId", userId);
                return FiberServer.get().getAuthService().getUserById(userId);
            }
        }
        return null;
    }
}
