package sh.fyz.fiber.core.security.filters;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;

/**
 * Sets the standard set of security headers on every non-{@code OPTIONS} response.
 *
 * <p>HSTS includes {@code preload} by default (a year max-age + subdomains, suitable
 * for the {@code hstspreload.org} list). Use {@link #setStrictTransportSecurity(String)}
 * to override (e.g. shorter max-age, no preload). The CSP defaults to
 * {@code default-src 'self'} but can be customised with
 * {@link #setContentSecurityPolicy(String)}.</p>
 */
public class SecurityHeadersFilter implements Filter {
    private static String serverHeader = "Fiber";
    private static String strictTransportSecurity = "max-age=31536000; includeSubDomains; preload";
    private static String contentSecurityPolicy = "default-src 'self'";
    private static String referrerPolicy = "strict-origin-when-cross-origin";
    private static String permissionsPolicy = "geolocation=(), microphone=(), camera=()";
    private static String crossOriginOpenerPolicy = "same-origin";

    public static void setServerHeader(String header) {
        serverHeader = header;
    }

    public static void setStrictTransportSecurity(String value) {
        strictTransportSecurity = value;
    }

    public static void setContentSecurityPolicy(String value) {
        contentSecurityPolicy = value;
    }

    public static void setReferrerPolicy(String value) {
        referrerPolicy = value;
    }

    public static void setPermissionsPolicy(String value) {
        permissionsPolicy = value;
    }

    public static void setCrossOriginOpenerPolicy(String value) {
        crossOriginOpenerPolicy = value;
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
            if (referrerPolicy != null) httpResponse.setHeader("Referrer-Policy", referrerPolicy);
            if (permissionsPolicy != null) httpResponse.setHeader("Permissions-Policy", permissionsPolicy);
            if (strictTransportSecurity != null) httpResponse.setHeader("Strict-Transport-Security", strictTransportSecurity);
            if (contentSecurityPolicy != null) httpResponse.setHeader("Content-Security-Policy", contentSecurityPolicy);
            if (crossOriginOpenerPolicy != null) httpResponse.setHeader("Cross-Origin-Opener-Policy", crossOriginOpenerPolicy);
        }

        chain.doFilter(request, response);
    }
}
