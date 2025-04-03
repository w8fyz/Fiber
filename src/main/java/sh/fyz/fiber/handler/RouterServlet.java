package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;

import java.lang.reflect.Method;
import java.io.IOException;

public class RouterServlet extends HttpServlet {
    private final EndpointRegistry endpointRegistry;

    public RouterServlet(EndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            // Find the matching endpoint
            String key = req.getRequestURI() + ":" + req.getMethod();
            EndpointHandler endpoint = endpointRegistry.getEndpoints().get(key);
            if (endpoint == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // Check rate limit before processing
            Object rateLimitResult = RateLimitProcessor.process(endpoint.getMethod(), endpoint.getParameters(), req);
            if (rateLimitResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) rateLimitResult;
                response.write(resp);
                return;
            }

            // Process the request
            endpoint.handleRequest(req, resp);
            
            // Reset rate limit on success
            RateLimitProcessor.onSuccess(endpoint.getMethod(), endpoint.getParameters(), req);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error: " + e.getMessage());
        }
    }
} 