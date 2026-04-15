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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;

public class RouterServlet extends HttpServlet {
    private static final Logger logger = LoggerFactory.getLogger(RouterServlet.class);

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

            EndpointHandler matchedEndpoint = null;
            Matcher matchedMatcher = null;

            // O(1) lookup for static routes
            String staticKey = requestUri + ":" + requestMethod;
            Map<String, EndpointHandler> endpoints = endpointRegistry.getEndpoints();
            EndpointHandler staticMatch = endpoints.get(staticKey);
            if (staticMatch != null && staticMatch.getPathVariableCount() == 0) {
                Matcher m = staticMatch.getPathPattern().matcher(requestUri);
                if (m.matches()) {
                    matchedEndpoint = staticMatch;
                    matchedMatcher = m;
                }
            }

            // Fallback to linear scan for dynamic routes
            if (matchedEndpoint == null) {
                String methodSuffix = ":" + requestMethod;
                for (Map.Entry<String, EndpointHandler> entry : endpoints.entrySet()) {
                    if (!entry.getKey().endsWith(methodSuffix)) {
                        continue;
                    }
                    EndpointHandler endpoint = entry.getValue();
                    Matcher m = endpoint.getPathPattern().matcher(requestUri);
                    if (m.matches()) {
                        if (matchedEndpoint == null || endpoint.getPathVariableCount() < matchedEndpoint.getPathVariableCount()) {
                            matchedEndpoint = endpoint;
                            matchedMatcher = m;
                        }
                        if (matchedEndpoint.getPathVariableCount() == 0) {
                            break;
                        }
                    }
                }
            }

            if (matchedEndpoint == null) {
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            // S-04: skip CORS headers if endpoint has @NoCors
            if (!matchedEndpoint.isNoCors()) {
                FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
                if (resp.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                    return;
                }
            }

            Method method = matchedEndpoint.getMethod();
            Object[] parameters = matchedEndpoint.getParameters();

            Object rateLimitResult = RateLimitProcessor.process(method, parameters, req);
            if (rateLimitResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) rateLimitResult;
                response.write(req, resp);
                return;
            }

            Object result = matchedEndpoint.handleRequest(req, resp, matchedMatcher);

            AuditLog auditLog = method.getAnnotation(AuditLog.class);
            if (auditLog != null) {
                AuditLogProcessor.logAuditEvent(req, resp, auditLog, method, parameters, result);
            }

            if (resp.getStatus() == 200) {
                RateLimitProcessor.onSuccess(method, req);
            }
        } catch (Exception e) {
            logger.error("Error processing request: {} {}", req.getMethod(), req.getRequestURI(), e);
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
