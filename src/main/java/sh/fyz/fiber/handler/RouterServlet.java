package sh.fyz.fiber.handler;

import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.log.FiberLog;
import sh.fyz.fiber.core.log.FiberLogger;
import sh.fyz.fiber.core.log.LogContext;
import sh.fyz.fiber.core.request.CompletedRequest;
import sh.fyz.fiber.core.request.RequestAttributes;
import sh.fyz.fiber.core.request.RequestListener;
import sh.fyz.fiber.core.security.logging.AuditContext;
import sh.fyz.fiber.core.session.SessionContext;

import java.lang.reflect.Method;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Matcher;

public class RouterServlet extends HttpServlet {
    private static final FiberLogger logger = FiberLog.get(RouterServlet.class);

    private final EndpointRegistry endpointRegistry;
    private final List<RequestListener> requestListeners;

    public RouterServlet(EndpointRegistry endpointRegistry) {
        this(endpointRegistry, List.of());
    }

    /**
     * @param requestListeners notified once per request; read on every request, so a concurrent
     *                         list lets listeners be added while the server runs
     */
    public RouterServlet(EndpointRegistry endpointRegistry, List<RequestListener> requestListeners) {
        this.endpointRegistry = endpointRegistry;
        this.requestListeners = Objects.requireNonNull(requestListeners, "requestListeners");
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
        long startedAt = System.currentTimeMillis();
        long startNanos = System.nanoTime();
        req.setAttribute(RequestAttributes.START_NANOS, startNanos);
        String requestId = LogContext.newRequestId();
        try {
            LogContext.callWith(requestId, () -> {
                boolean handled = false;
                try {
                    doService(req, resp);
                    handled = true;
                } finally {
                    // An exception escaping doService is answered 500 by Jetty, unless a status was already sent.
                    int status = handled || resp.isCommitted()
                            ? resp.getStatus()
                            : HttpServletResponse.SC_INTERNAL_SERVER_ERROR;
                    notifyRequestListeners(req, status, requestId, startedAt, startNanos);
                }
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

            req.setAttribute(RequestAttributes.ROUTE, matchedEndpoint.getRoute());

            if (!matchedEndpoint.isNoCors()) {
                FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
                if (resp.getStatus() == HttpServletResponse.SC_FORBIDDEN) {
                    return;
                }
            }

            if (logger.isDebugEnabled()) {
                Method method = matchedEndpoint.getMethod();
                logger.debug("dispatch {} {} -> {}.{}", requestMethod, requestUri,
                        method.getDeclaringClass().getSimpleName(), method.getName());
            }

            matchedEndpoint.handleRequest(req, resp, matchedMatcher);
        } catch (IllegalArgumentException e) {
            logger.debug("Bad request: {} {} — {}", req.getMethod(), req.getRequestURI(), e.getMessage());
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_BAD_REQUEST);
                resp.setContentType("application/json");
                String body = "{\"status\":400,\"message\":" + jsonEscape(e.getMessage()) + "}";
                resp.getOutputStream().write(body.getBytes());
            }
        } catch (Exception e) {
            FiberLog.handle(e, "Unhandled error processing {} {}", req.getMethod(), req.getRequestURI());
            if (!resp.isCommitted()) {
                resp.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
                resp.setContentType("application/json");
                resp.getOutputStream().write("{\"status\":500,\"message\":\"Could not process the request right now, please try again.\"}".getBytes());
            }
        } finally {
            AuditContext.clear();
            SessionContext.clear();
        }
    }

    private void notifyRequestListeners(HttpServletRequest req, int status, String requestId,
                                        long startedAt, long startNanos) {
        if (requestListeners.isEmpty()) {
            return;
        }
        Object route = req.getAttribute(RequestAttributes.ROUTE);
        CompletedRequest completed = new CompletedRequest(
                requestId,
                req.getMethod(),
                req.getRequestURI(),
                route instanceof String r ? r : null,
                status,
                startedAt,
                System.nanoTime() - startNanos,
                req);
        for (RequestListener listener : requestListeners) {
            try {
                listener.onRequestCompleted(completed);
            } catch (VirtualMachineError e) {
                throw e;
            } catch (Throwable t) {
                // Errors too (a metrics library failing to initialise throws NoClassDefFoundError on
                // every call): escaping here would turn an uncommitted response into a 500.
                FiberLog.handle(t, "Request listener {} failed for {} {}",
                        listener.getClass().getName(), completed.method(), completed.path());
            }
        }
    }
}
