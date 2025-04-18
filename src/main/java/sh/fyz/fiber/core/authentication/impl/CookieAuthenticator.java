package sh.fyz.fiber.core.authentication.impl;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.Authenticator;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.JwtUtil;

public class CookieAuthenticator implements Authenticator {
    private static final String ACCESS_TOKEN_COOKIE = "access_token";

    @Override
    public AuthScheme scheme() {
        return AuthScheme.COOKIE;
    }

    @Override
    public UserAuth authenticate(HttpServletRequest request) {
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                    String token = cookie.getValue();
                    if (JwtUtil.validateToken(token, request.getRemoteAddr(), request.getHeader("User-Agent"))) {
                        return FiberServer.get().getAuthService().getUserById(AuthMiddleware.getCurrentUserId(request));
                    }
                    break;
                }
            }
        }
        return null;
    }
} 