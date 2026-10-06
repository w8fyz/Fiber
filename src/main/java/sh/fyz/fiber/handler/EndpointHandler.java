package sh.fyz.fiber.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.NoCors;
import sh.fyz.fiber.annotations.security.NoCSRF;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.interceptors.RateLimitInterceptor;
import sh.fyz.fiber.core.security.logging.AuditLogProcessor;
import sh.fyz.fiber.core.security.processors.RateLimitProcessor;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.middleware.Middleware;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public class EndpointHandler {
    private final Object controller;
    private final Method method;
    private final Parameter[] parameters;
    private final RateLimit rateLimit;
    private final AuditLog auditLog;
    /** Resolved on first use, so handlers registered after the controller are still seen. */
    private volatile ParameterHandler[] parameterHandlers;
    private final Pattern pathPattern;
    private final int pathVariableCount;

    private final boolean noCors;
    private final SecurityPipeline securityPipeline;
    private final List<Middleware> globalMiddleware;

    public EndpointHandler(Object controller, Method method, List<Middleware> globalMiddleware, String[] requiredRoles) {
        this.controller = controller;
        this.method = method;
        this.parameters = method.getParameters();
        this.rateLimit = RateLimitInterceptor.resolveRateLimit(method);
        this.auditLog = method.getAnnotation(AuditLog.class);

        this.noCors = method.isAnnotationPresent(NoCors.class);
        boolean noCsrf = method.isAnnotationPresent(NoCSRF.class);

        boolean basicAuth = false;
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType() == OAuth2ApplicationInfo.class) {
                basicAuth = true;
                break;
            }
        }

        Set<AuthScheme> acceptedAuthSchemes = computeAcceptedAuthSchemes(method);
        this.securityPipeline = new SecurityPipeline(method, noCsrf, basicAuth, acceptedAuthSchemes);

        this.globalMiddleware = globalMiddleware;

        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        Controller controllerAnnotation = method.getDeclaringClass().getAnnotation(Controller.class);
        String basePath = controllerAnnotation != null ? controllerAnnotation.value() : "";
        String path = normalizePath(basePath + mapping.value());

        String regex = path
            .replaceAll("\\*", ".*")
            .replaceAll("\\{([^}]+)}", "(?<$1>[^/]+)");
        this.pathPattern = Pattern.compile("^" + regex + "$");

        Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(path);
        int varCount = 0;
        while (matcher.find()) varCount++;
        this.pathVariableCount = varCount;
    }

    private static Set<AuthScheme> computeAcceptedAuthSchemes(Method method) {
        AuthType authType = method.getAnnotation(AuthType.class);
        if (authType != null) {
            Set<AuthScheme> schemes = new HashSet<>(Arrays.asList(authType.value()));
            for (Parameter parameter : method.getParameters()) {
                if (parameter.getType() == OAuth2ApplicationInfo.class) {
                    schemes.remove(AuthScheme.BASIC);
                    break;
                }
            }
            return schemes;
        }

        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                return new HashSet<>(Collections.singletonList(AuthScheme.COOKIE));
            }
        }

        boolean requiresAuth = method.isAnnotationPresent(sh.fyz.fiber.annotations.security.RequireRole.class)
                || method.isAnnotationPresent(sh.fyz.fiber.annotations.security.Permission.class)
                || method.getDeclaringClass().isAnnotationPresent(sh.fyz.fiber.annotations.security.RequireRole.class)
                || method.getDeclaringClass().isAnnotationPresent(sh.fyz.fiber.annotations.security.Permission.class);
        if (requiresAuth) {
            return new HashSet<>(Set.of(AuthScheme.COOKIE, AuthScheme.BEARER));
        }

        return Collections.emptySet();
    }

    public Object handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        return handleRequest(req, resp, null);
    }

    public Object handleRequest(HttpServletRequest req, HttpServletResponse resp, Matcher precomputedMatcher) throws ServletException, IOException {
        // IP-keyed limits run before authentication so failed attempts are counted too.
        if (rateLimit != null && !rateLimit.perUser() && rejectRateLimited(req, resp)) {
            return null;
        }

        Invocation invocation = invoke(req, resp, precomputedMatcher);
        if (invocation == Invocation.RATE_LIMITED) {
            return null;
        }

        if (auditLog != null) {
            AuditLogProcessor.logAuditEvent(req, resp, auditLog, method, invocation.args(), invocation.result());
        }
        if (rateLimit != null && resp.getStatus() == HttpServletResponse.SC_OK) {
            RateLimitProcessor.reset(rateLimit, method, invocation.args(), req);
        }
        return invocation.result();
    }

    private Invocation invoke(HttpServletRequest req, HttpServletResponse resp, Matcher precomputedMatcher) throws ServletException, IOException {
        SecurityResult security = securityPipeline.execute(req, resp);
        if (!security.shouldProceed()) {
            return Invocation.NONE;
        }

        // Per-user limits need the identity resolved by the security pipeline.
        if (rateLimit != null && rateLimit.perUser() && rejectRateLimited(req, resp)) {
            return Invocation.RATE_LIMITED;
        }

        for (Middleware middleware : globalMiddleware) {
            if (!middleware.handle(req, resp)) {
                return Invocation.NONE;
            }
        }

        String path = req.getRequestURI();
        Matcher matcher = precomputedMatcher;
        if (matcher == null) {
            matcher = pathPattern.matcher(path);
            if (!matcher.matches()) {
                ErrorResponse.send(resp, path, HttpServletResponse.SC_NOT_FOUND, "Path not found");
                return Invocation.NONE;
            }
        }

        try {
            Object[] args = ParameterResolver.resolve(parameters, parameterHandlers(), req, resp, matcher,
                    security.getAuthenticatedUser(), security.getAuthenticatedApp());
            Object result = method.invoke(controller, args);
            ResponseWriter.write(result, req, resp);
            return new Invocation(args, result);
        } catch (ParameterResolver.ResolveException e) {
            ErrorResponse.send(resp, path, e.getStatusCode(), e.getMessage());
            return Invocation.NONE;
        } catch (Exception e) {
            throw new ServletException("Failed to invoke endpoint method", e);
        }
    }

    private boolean rejectRateLimited(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        ResponseEntity<?> tooManyRequests = RateLimitProcessor.check(rateLimit, method, null, req);
        if (tooManyRequests == null) {
            return false;
        }
        tooManyRequests.write(req, resp);
        return true;
    }

    private ParameterHandler[] parameterHandlers() {
        ParameterHandler[] handlers = parameterHandlers;
        if (handlers == null) {
            handlers = ParameterResolver.findHandlers(parameters);
            parameterHandlers = handlers;
        }
        return handlers;
    }

    private record Invocation(Object[] args, Object result) {
        static final Invocation NONE = new Invocation(null, null);
        static final Invocation RATE_LIMITED = new Invocation(null, null);
    }

    public Method getMethod() {
        return method;
    }

    public Parameter[] getParameters() {
        return parameters.clone();
    }

    public boolean matchesPath(String requestUri) {
        return pathPattern.matcher(requestUri).matches();
    }

    public int getPathVariableCount() {
        return pathVariableCount;
    }

    public boolean isNoCors() {
        return noCors;
    }

    public Pattern getPathPattern() {
        return pathPattern;
    }

    public static String normalizePath(String path) {
        String normalized = path.replaceAll("/{2,}", "/");
        if (normalized.length() > 1 && normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        if (normalized.isEmpty()) {
            normalized = "/";
        }
        return normalized;
    }
}
