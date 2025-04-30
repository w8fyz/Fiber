package sh.fyz.fiber.core.authentication;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

/**
 * Middleware for handling authentication.
 */
public class AuthMiddleware {
    private static final String USER_ID_ATTRIBUTE = "userId";

    public static boolean process(HttpServletRequest req, HttpServletResponse resp) {

        Cookie[] cookies = req.getCookies();
        String token = null;
        
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("access_token".equals(cookie.getName())) {
                    token = cookie.getValue();
                    break;
                }
            }
        }

        if (token == null) {
            ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "No valid token provided");
            return false;
        }

        try {
            if (!FiberServer.get().getAuthService().validateToken(token, req)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid token or browser mismatch");
                return false;
            }

            // Extract user info from token
            Object userId = JwtUtil.extractId(token);
            System.out.println("Extracted user ID: " + userId);
            req.setAttribute(USER_ID_ATTRIBUTE, userId);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return false;
        }
    }
    /**
     * Get the current user from the request attributes
     */
    public static UserAuth getCurrentUser(HttpServletRequest request) {
        Object userId = request.getAttribute(USER_ID_ATTRIBUTE);
        if (userId != null) {
            return FiberServer.get().getAuthService().getUserById(userId);
        }
        return null;
    }

    /**
     * Get the current user ID from the request attributes
     */
    public static Object getCurrentUserId(HttpServletRequest req) {
        System.out.println("Getting current user ID from request attributes: " + req.getAttribute(USER_ID_ATTRIBUTE));
        return req.getAttribute(USER_ID_ATTRIBUTE);
    }
} 