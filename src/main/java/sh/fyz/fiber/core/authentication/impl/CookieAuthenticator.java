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
        System.out.println("---- AUTHENTICATION START ----");

        Cookie[] cookies = request.getCookies();
        if (cookies == null) {
            System.out.println("No cookies found in request");
            System.out.println("---- AUTHENTICATION END (no cookies) ----");
            return null;
        }

        System.out.println("Found " + cookies.length + " cookies");
        for (Cookie cookie : cookies) {
            System.out.println("Checking cookie: " + cookie.getName());
            if (ACCESS_TOKEN_COOKIE.equals(cookie.getName())) {
                String token = cookie.getValue();
                System.out.println("Found access token cookie: " + token);

                boolean valid = JwtUtil.validateToken(token, request.getRemoteAddr(), request.getHeader("User-Agent"));
                System.out.println("Token validation result: " + valid);

                if (valid) {
                    Object userId = JwtUtil.extractId(token);
                    System.out.println("Extracted userId from token: " + userId);
                    request.setAttribute("userId", userId);

                    UserAuth user = FiberServer.get().getAuthService().getUserById(userId);
                    System.out.println("Fetched user from AuthService: " + user);
                    System.out.println("---- AUTHENTICATION END (success) ----");
                    return user;
                } else {
                    System.out.println("Token invalid for cookie: " + ACCESS_TOKEN_COOKIE);
                    break;
                }
            }
        }

        System.out.println("Access token cookie not found or invalid");
        System.out.println("---- AUTHENTICATION END (unauthorized) ----");
        return null;
    }

} 