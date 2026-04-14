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
        
        httpResponse.setHeader("Server", serverHeader);
        
        if (!httpRequest.getMethod().equals("OPTIONS")) {
            httpResponse.setHeader("X-Content-Type-Options", "nosniff");
            httpResponse.setHeader("X-Frame-Options", "DENY");
            httpResponse.setHeader("Referrer-Policy", "strict-origin-when-cross-origin");
            httpResponse.setHeader("Permissions-Policy", "geolocation=(), microphone=(), camera=()");
            httpResponse.setHeader("Strict-Transport-Security", "max-age=31536000; includeSubDomains");
            httpResponse.setHeader("Content-Security-Policy", "default-src 'self'");
            httpResponse.setHeader("Cross-Origin-Opener-Policy", "same-origin");
        }
        
        chain.doFilter(request, response);
    }
}
