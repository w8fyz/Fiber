package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogProcessor;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Map;

public class RouterServlet extends HttpServlet {
    private final EndpointRegistry endpointRegistry;

    public RouterServlet(EndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String requestUri = req.getRequestURI();
            String requestMethod = req.getMethod();

            // Gérer CORS en premier
            if (requestMethod.equals("OPTIONS")) {
                FiberServer.get().getCorsService().handlePreflightRequest(req, resp);
                return;
            }

            // Vérifier les en-têtes CORS pour toutes les autres requêtes
            // Si l'origine n'est pas autorisée, configureCorsHeaders renverra un 403
            FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
            if (resp.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                return;
            }

            // Continuer le traitement seulement si CORS est OK
            EndpointHandler matchedEndpoint = null;
            
            // Try to find a matching endpoint
            for (Map.Entry<String, EndpointHandler> entry : endpointRegistry.getEndpoints().entrySet()) {
                String key = entry.getKey();
                EndpointHandler endpoint = entry.getValue();
                
                // Check if the HTTP method matches
                if (!key.endsWith(":" + requestMethod)) {
                    continue;
                }
                
                // Extract the path pattern from the key
                String pathPattern = key.substring(0, key.lastIndexOf(":"));
                
                // Check if the path matches the pattern
                if (endpoint.matchesPath(requestUri)) {
                    matchedEndpoint = endpoint;
                    break;
                }
            }
            
            if (matchedEndpoint == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Method method = matchedEndpoint.getMethod();
            Object[] parameters = matchedEndpoint.getParameters();

            // Check rate limit before processing

            Object rateLimitResult = RateLimitProcessor.process(method, parameters, req);
            if (rateLimitResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) rateLimitResult;
                response.write(req, resp);
                return;
            }

            // Process the request
            Object result = matchedEndpoint.handleRequest(req, resp);
            
            // Log audit event if @AuditLog annotation is present
            AuditLog auditLog = method.getAnnotation(AuditLog.class);
            if (auditLog != null) {
                AuditLogProcessor.logAuditEvent(req, resp, auditLog, method, parameters, result);
            }

            if(resp.getStatus() == 200) { //200 Means success, we reset the RateLimit
                RateLimitProcessor.onSuccess(method, req);
            }
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            e.printStackTrace();
            resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            resp.getOutputStream().write(("Internal server error: " + e.getMessage()).getBytes());
        }
    }
} 