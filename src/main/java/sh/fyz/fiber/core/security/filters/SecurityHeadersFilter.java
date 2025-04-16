package sh.fyz.fiber.core.security.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public class SecurityHeadersFilter implements Filter {
    private static String serverHeader = "Fiber - A Fyz Project";

    public static void setServerHeader(String header) {
        serverHeader = header;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        
        // Set server header
        httpResponse.setHeader("Server", serverHeader);
        
        // N'applique pas les en-têtes de sécurité pour les requêtes OPTIONS (CORS preflight)
        if (!httpRequest.getMethod().equals("OPTIONS")) {
            // Basic security headers
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("X-XSS-Protection", "1; mode=block");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
        }
        
        chain.doFilter(request, response);
    }
} 