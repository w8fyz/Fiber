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

import sh.fyz.fiber.core.security.logging.AuditContext;
import sh.fyz.fiber.core.session.SessionContext;

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

            if (requestMethod.equals("OPTIONS")) {
                FiberServer.get().getCorsService().handlePreflightRequest(req, resp);
                return;
            }

            FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
            if (resp.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                return;
            }

            EndpointHandler matchedEndpoint = null;

            String methodSuffix = ":" + requestMethod;
            for (Map.Entry<String, EndpointHandler> entry : endpointRegistry.getEndpoints().entrySet()) {
                if (!entry.getKey().endsWith(methodSuffix)) {
                    continue;
                }
                EndpointHandler endpoint = entry.getValue();
                if (endpoint.matchesPath(requestUri)) {
                    if (matchedEndpoint == null || endpoint.getPathVariableCount() < matchedEndpoint.getPathVariableCount()) {
                        matchedEndpoint = endpoint;
                    }
                    if (matchedEndpoint.getPathVariableCount() == 0) {
                        break;
                    }
                }
            }

            if (matchedEndpoint == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            Method method = matchedEndpoint.getMethod();
            Object[] parameters = matchedEndpoint.getParameters();

            Object rateLimitResult = RateLimitProcessor.process(method, parameters, req);
            if (rateLimitResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) rateLimitResult;
                response.write(req, resp);
                return;
            }

            Object result = matchedEndpoint.handleRequest(req, resp);

            AuditLog auditLog = method.getAnnotation(AuditLog.class);
            if (auditLog != null) {
                AuditLogProcessor.logAuditEvent(req, resp, auditLog, method, parameters, result);
            }

            if (resp.getStatus() == 200) {
                RateLimitProcessor.onSuccess(method, req);
            }
        } catch (Exception e) {
            System.err.println("Error processing request: " + e.getMessage());
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.getOutputStream().write("{\"status\":500,\"message\":\"Could not process the request right now, please try again.\"}".getBytes());
            }
        } finally {
            AuditContext.clear();
            SessionContext.clear();
        }
    }
} 