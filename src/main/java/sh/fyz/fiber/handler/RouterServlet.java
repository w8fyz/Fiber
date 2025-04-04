package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogger;

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

            Method method = endpoint.getMethod();
            Object[] parameters = endpoint.getParameters();

            // Check rate limit before processing
            Object rateLimitResult = RateLimitProcessor.process(method, parameters, req);
            if (rateLimitResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) rateLimitResult;
                response.write(resp);
                return;
            }

            // Process the request
            Object result = endpoint.handleRequest(req, resp);
            
            // Log audit event if @AuditLog annotation is present
            AuditLog auditLog = method.getAnnotation(AuditLog.class);
            if (auditLog != null) {
                AuditLogger.logAuditEvent(auditLog, method, parameters, result);
            }
            
            // Reset rate limit on success
            RateLimitProcessor.onSuccess(method, parameters, req);
        } catch (Exception e) {
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getWriter().write("Internal server error: " + e.getMessage());
        }
    }
} 