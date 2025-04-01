package sh.fyz.fiber.core;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Middleware for handling authentication using Bearer tokens.
 */
public class AuthMiddleware {
    private static final String AUTHORIZATION_HEADER = "Authorization";
    private static final String BEARER_PREFIX = "Bearer ";

    /**
     * Process the request and validate the Bearer token.
     * If the token is valid, it adds the user information to the request attributes.
     * If the token is invalid or missing, it returns a 401 Unauthorized response.
     *
     * @param request The HTTP request
     * @param response The HTTP response
     * @return true if the request should continue, false if it should stop
     */
    public static boolean process(HttpServletRequest request, HttpServletResponse response) throws IOException {
        String authHeader = request.getHeader(AUTHORIZATION_HEADER);
        
        if (authHeader == null || !authHeader.startsWith(BEARER_PREFIX)) {
            sendUnauthorizedResponse(response, "No Bearer token provided");
            return false;
        }

        String token = authHeader.substring(BEARER_PREFIX.length());
        
        if (!JwtUtil.validateToken(token)) {
            sendUnauthorizedResponse(response, "Invalid token");
            return false;
        }

        // Add user information to request attributes
        request.setAttribute("userId", JwtUtil.extractId(token));
        request.setAttribute("username", JwtUtil.extractUsername(token));
        request.setAttribute("userRole", JwtUtil.extractRole(token));

        return true;
    }

    /**
     * Send an unauthorized response with a JSON error message
     */
    private static void sendUnauthorizedResponse(HttpServletResponse response, String message) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");
        response.getWriter().write(String.format("{\"error\": \"%s\"}", message));
    }

    /**
     * Get the current user ID from the request attributes
     */
    public static String getCurrentUserId(HttpServletRequest request) {
        return (String) request.getAttribute("userId");
    }

    /**
     * Get the current username from the request attributes
     */
    public static String getCurrentUsername(HttpServletRequest request) {
        return (String) request.getAttribute("username");
    }

    /**
     * Get the current user role from the request attributes
     */
    public static String getCurrentUserRole(HttpServletRequest request) {
        return (String) request.getAttribute("userRole");
    }
} 