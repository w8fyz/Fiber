package sh.fyz.fiber.core.authentication.impl;

import io.jsonwebtoken.Claims;
import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.Authenticator;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.core.session.SessionContext;
import sh.fyz.fiber.core.session.SessionService;

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
                if (!validateSession(claims)) {
                    return null;
                }

                Object userId = claims.get("id");
                request.setAttribute("userId", userId);
                return FiberServer.get().getAuthService().getUserById(userId);
            }
        }
        return null;
    }

    private boolean validateSession(Claims claims) {
        String sessionId = claims.get("sessionId", String.class);
        if (sessionId == null) {
            return true;
        }

        SessionService sessionService = FiberServer.get().getSessionService();
        if (sessionService == null) {
            return true;
        }

        FiberSession session = sessionService.getSession(sessionId);
        if (session == null) {
            return false;
        }

        SessionContext.set(session);
        return true;
    }
}
