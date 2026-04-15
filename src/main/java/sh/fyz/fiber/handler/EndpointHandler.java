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
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.middleware.Middleware;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

public class EndpointHandler {
    private final Object controller;
    private final Method method;
    private final Pattern pathPattern;
    private final int pathVariableCount;

    private final boolean noCors;
    private final SecurityPipeline securityPipeline;
    private final List<Middleware> sortedMiddleware;

    public EndpointHandler(Object controller, Method method, List<Middleware> globalMiddleware, String[] requiredRoles) {
        this.controller = controller;
        this.method = method;

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

        List<Middleware> sorted = new ArrayList<>(globalMiddleware);
        sorted.sort(Comparator.comparingInt(Middleware::priority));
        this.sortedMiddleware = Collections.unmodifiableList(sorted);

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
        SecurityResult security = securityPipeline.execute(req, resp);
        if (!security.shouldProceed()) {
            return null;
        }

        for (Middleware middleware : sortedMiddleware) {
            if (!middleware.handle(req, resp)) {
                return null;
            }
        }

        String path = req.getRequestURI();
        Matcher matcher = precomputedMatcher;
        if (matcher == null) {
            matcher = pathPattern.matcher(path);
            if (!matcher.matches()) {
                ErrorResponse.send(resp, path, HttpServletResponse.SC_NOT_FOUND, "Path not found");
                return null;
            }
        }

        try {
            Object[] args = ParameterResolver.resolve(method, req, resp, matcher, security.getAuthenticatedUser());
            Object result = method.invoke(controller, args);
            ResponseWriter.write(result, req, resp);
            return result;
        } catch (ParameterResolver.ResolveException e) {
            ErrorResponse.send(resp, path, e.getStatusCode(), e.getMessage());
            return null;
        } catch (Exception e) {
            throw new ServletException("Failed to invoke endpoint method", e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public Parameter[] getParameters() {
        return method.getParameters();
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
