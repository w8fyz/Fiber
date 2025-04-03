package sh.fyz.fiber.core.authentication;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;

/**
 * Middleware for handling authentication using Bearer tokens.
 */
public class AuthMiddleware {
    private static final String USER_ID_ATTRIBUTE = "userId";

    public static boolean process(HttpServletRequest req, HttpServletResponse resp) {
        String token = req.getHeader("Authorization");
        if (token == null || !token.startsWith("Bearer ")) {
            ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "No valid token provided");
            return false;
        }

        token = token.substring(7); // Remove "Bearer " prefix
        try {
            if (!FiberServer.get().getAuthService().validateToken(token, req)) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid token or browser mismatch");
                return false;
            }

            // Extract user info from token
            String userId = JwtUtil.extractId(token);
            req.setAttribute(USER_ID_ATTRIBUTE, userId);

            return true;
        } catch (Exception e) {
            ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid token");
            return false;
        }
    }
    /**
     * Get the current user from the request attributes
     */
    public static UserAuth getCurrentUser(HttpServletRequest req) {
        String userId = (String) req.getAttribute(USER_ID_ATTRIBUTE);
        if (userId == null) {
            return null;
        }
        return FiberServer.get().getAuthService().getUserById(userId);
    }

    /**
     * Get the current user ID from the request attributes
     */
    public static Object getCurrentUserId(HttpServletRequest req) {
        return req.getAttribute(USER_ID_ATTRIBUTE);
    }

    /**
     * Get the current username from the request attributes
     */
    public static String getCurrentUsername(HttpServletRequest req) {
        UserAuth userAuth = getCurrentUser(req);
        return userAuth != null ? userAuth.getUsername() : null;
    }

    /**
     * Get the current user role from the request attributes
     */
    public static String getCurrentUserRole(HttpServletRequest req) {
        UserAuth userAuth = getCurrentUser(req);
        return userAuth != null ? userAuth.getRole() : null;
    }
} 