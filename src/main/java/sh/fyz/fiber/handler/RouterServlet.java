package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.LogContext;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.logging.AuditLogProcessor;
import sh.fyz.fiber.core.security.logging.AuditContext;
import sh.fyz.fiber.core.session.SessionContext;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.Map;
import java.util.regex.Matcher;

public class RouterServlet extends HttpServlet {
    private static final FiberLogger logger = FiberLog.get(RouterServlet.class);

    private final EndpointRegistry endpointRegistry;

    public RouterServlet(EndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    private static String jsonEscape(String s) {
        if (s == null) return "\"\"";
        StringBuilder sb = new StringBuilder(s.length() + 16).append('"');
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            switch (c) {
                case '"' -> sb.append("\\\"");
                case '\\' -> sb.append("\\\\");
                case '\n' -> sb.append("\\n");
                case '\r' -> sb.append("\\r");
                case '\t' -> sb.append("\\t");
                default -> {
                    if (c < 0x20) sb.append(String.format("\\u%04x", (int) c));
                    else sb.append(c);
                }
            }
        }
        sb.append('"');
        return sb.toString();
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        String requestId = LogContext.newRequestId();
        try {
            LogContext.callWith(requestId, () -> {
                doService(req, resp);
                return null;
            });
        } catch (IOException ioe) {
            throw ioe;
        } catch (RuntimeException re) {
            throw re;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    private void doService(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        try {
            String requestUri = req.getRequestURI();
            String requestMethod = req.getMethod();

            if (logger.isTraceEnabled()) {
                logger.trace("rx {} {}", requestMethod, requestUri);
            }

            if (requestMethod.equals("OPTIONS")) {
                FiberServer.get().getCorsService().handlePreflightRequest(req, resp);
                return;
            }

            EndpointHandler matchedEndpoint = null;
            Matcher matchedMatcher = null;

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

            if (matchedEndpoint == null) {
                EndpointHandler trieMatch = endpointRegistry.getDynamicTrie().find(requestUri, requestMethod);
                if (trieMatch != null) {
                    Matcher m = trieMatch.getPathPattern().matcher(requestUri);
                    if (m.matches()) {
                        matchedEndpoint = trieMatch;
                        matchedMatcher = m;
                    }
                }
            }

            if (matchedEndpoint == null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("no route for {} {}", requestMethod, requestUri);
                }
                resp.setStatus(HttpServletResponse.SC_NOT_FOUND);
                return;
            }

            if (!matchedEndpoint.isNoCors()) {
                FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
                if (resp.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                    return;
                }
            }

            Method method = matchedEndpoint.getMethod();
            Object[] parameters = matchedEndpoint.getParameters();

            if (logger.isDebugEnabled()) {
                logger.debug("dispatch {} {} -> {}.{}", requestMethod, requestUri,
                        method.getDeclaringClass().getSimpleName(), method.getName());
            }

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
        } catch (IllegalArgumentException e) {
            logger.debug("Bad request: {} {} — {}", req.getMethod(), req.getRequestURI(), e.getMessage());
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                String body = "{\"status\":400,\"message\":" + jsonEscape(e.getMessage()) + "}";
                resp.getOutputStream().write(body.getBytes());
            }
        } catch (Exception e) {
            FiberLog.handle(e, "Unhandled error processing {} {}", req.getMethod(), req.getRequestURI());
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
