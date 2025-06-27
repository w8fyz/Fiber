package sh.fyz.fiber.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.NoCors;
import sh.fyz.fiber.annotations.security.NoCSRF;
import sh.fyz.fiber.annotations.security.Permission;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.AuthResolver;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.Role;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.security.processors.PermissionProcessor;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;
import sh.fyz.fiber.core.authentication.RoleRegistry;

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

public class EndpointHandler extends HttpServlet {
    private final Object controller;
    private final Method method;
    private final List<Middleware> globalMiddleware;
    private final String[] requiredRoles;
    private final String[] requiredPermissions;
    private final Pattern pathPattern;
    private String[] pathVariableNames;

    public EndpointHandler(Object controller, Method method, List<Middleware> globalMiddleware, String[] requiredRoles) {
        this.controller = controller;
        this.method = method;
        this.globalMiddleware = globalMiddleware;
        this.requiredRoles = requiredRoles;
        
        // Get required permissions from the Permission annotation
        Permission permissionAnnotation = method.getAnnotation(Permission.class);
        this.requiredPermissions = permissionAnnotation != null ? permissionAnnotation.value() : new String[0];

        // Extract path variables from the request mapping
        RequestMapping mapping = method.getAnnotation(RequestMapping.class);
        Controller controllerAnnotation = method.getDeclaringClass().getAnnotation(Controller.class);
        String basePath = controllerAnnotation != null ? controllerAnnotation.value() : "";
        String path = basePath + mapping.value();
        
        // Convert path to regex pattern
        String regex = path
            .replaceAll("\\*", ".*")  // Replace * with regex wildcard
            .replaceAll("\\{([^}]+)}", "(?<$1>[^/]+)"); // Replace {param} with capture group
        this.pathPattern = Pattern.compile("^" + regex + "$");
        
        // Extract path variable names
        Matcher matcher = Pattern.compile("\\{([^}]+)}").matcher(path);
        this.pathVariableNames = new String[0];
        if (matcher.find()) {
            this.pathVariableNames = new String[matcher.groupCount()];
            matcher.reset();
            int i = 0;
            while (matcher.find()) {
                this.pathVariableNames[i++] = matcher.group(1);
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.GET) {
                try {
                    handleRequest(req, resp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.POST) {
                try {
                    handleRequest(req, resp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) {
        System.out.println("POST DETECTED");
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.PUT) {
                try {
                    handleRequest(req, resp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.DELETE) {
                try {
                    handleRequest(req, resp);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doOptions(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Handling OPTIONS request");
        // Execute global middleware (which includes CORS)
        List<Middleware> sortedMiddleware = new ArrayList<>(globalMiddleware);
        Collections.sort(sortedMiddleware, Comparator.comparingInt(Middleware::priority));
        for (Middleware middleware : sortedMiddleware) {
            System.out.println("Executing middleware for OPTIONS: " + middleware.getClass().getSimpleName());
            if (!middleware.handle(req, resp)) {
                return;
            }
        }
    }

    private boolean shouldApplyCors(Method method) {
        return !method.isAnnotationPresent(NoCors.class);
    }

    private boolean shouldApplyCsrf(Method method) {
        return !method.isAnnotationPresent(NoCSRF.class);
    }

    private Set<AuthScheme> getAcceptedAuthSchemes(Method method) {
        AuthType authType = method.getAnnotation(AuthType.class);
        if (authType != null) {
            Set<AuthScheme> schemes = new HashSet<>(Arrays.asList(authType.value()));
            // Remove BASIC from user auth schemes if we have OAuth2ApplicationInfo parameter
            if (requiresBasicAuth(method)) {
                schemes.remove(AuthScheme.BASIC);
            }
            return schemes;
        }
        
        // Si pas d'AuthType mais @AuthenticatedUser est présent, utiliser COOKIE par défaut
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                return new HashSet<>(Collections.singletonList(AuthScheme.COOKIE));
            }
        }
        
        return Collections.emptySet();
    }

    private boolean requiresBasicAuth(Method method) {
        for (Parameter parameter : method.getParameters()) {
            if (parameter.getType() == OAuth2ApplicationInfo.class) {
                return true;
            }
        }
        return false;
    }

    public Object handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Apply CORS if not disabled
        if (shouldApplyCors(method)) {
            FiberServer.get().getCorsService().configureCorsHeaders(req, resp);
            if (req.getMethod().equals("OPTIONS")) {
                FiberServer.get().getCorsService().handlePreflightRequest(req, resp);
                return null;
            }
        }

        // Apply CSRF if not disabled
        if (shouldApplyCsrf(method) && FiberServer.get().getCsrfMiddleware() != null) {
            if (!FiberServer.get().getCsrfMiddleware().handle(req, resp)) {
                return null;
            }
        }

        // Handle OAuth2 application authentication first
        OAuth2ApplicationInfo authenticatedApp = null;
        if (requiresBasicAuth(method)) {
            authenticatedApp = FiberServer.get().getBasicAuthenticator().authenticate(req);
            if (authenticatedApp == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Invalid client credentials");
                return null;
            }
        }

        // Then handle user authentication if needed
        Set<AuthScheme> acceptedSchemes = getAcceptedAuthSchemes(method);
        UserAuth authenticatedUser = null;
        if (!acceptedSchemes.isEmpty()) {
            authenticatedUser = FiberServer.get().getAuthResolver().resolveUser(req, acceptedSchemes);
            if (authenticatedUser == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return null;
            }
            // Store the authenticated user ID in request attributes for PermissionProcessor
            req.setAttribute("userId", authenticatedUser.getId());
        }

        // Execute global middleware
        System.out.println("EXECUTE GLOBAL MIDDLEWARES");
        List<Middleware> sortedMiddleware = new ArrayList<>(globalMiddleware);
        System.out.println("Size : " + sortedMiddleware.size());
        Collections.sort(sortedMiddleware, Comparator.comparingInt(Middleware::priority));
        for (Middleware middleware : sortedMiddleware) {
            System.out.println("Executing middleware: " + middleware.getClass().getSimpleName());
            if (!middleware.handle(req, resp)) {
                return null;
            }
        }

        // Extract path variables
        String path = req.getRequestURI();
        Matcher matcher = pathPattern.matcher(path);
        if (!matcher.matches()) {
            System.out.println("Path not found: " + path);
            ErrorResponse.send(resp, path, HttpServletResponse.SC_NOT_FOUND, "Path not found");
            return null;
        }

        try {
            // Prepare method arguments using parameter handlers
            Object[] args = new Object[method.getParameterCount()];
            for (int i = 0; i < method.getParameterCount(); i++) {
                Parameter parameter = method.getParameters()[i];
                if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                    args[i] = authenticatedUser;
                } else {
                    ParameterHandler handler = ParameterHandlerRegistry.findHandler(parameter);
                    if (handler == null) {
                        throw new IllegalArgumentException("No handler found for parameter: " + parameter.getName());
                    }
                    try {
                        args[i] = handler.handle(parameter, req, resp, matcher);
                    } catch (Exception e) {
                        if (e instanceof IllegalArgumentException) {
                            ErrorResponse.send(resp, path, HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
                        } else {
                            e.printStackTrace();
                            ErrorResponse.send(resp, path, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                        }
                        return null;
                    }
                }
            }

            Object permissionResult = PermissionProcessor.process(method, null, req);
            if (permissionResult != null) {
                ResponseEntity<?> response = (ResponseEntity<?>) permissionResult;
                response.write(req, resp);
                return null;
            }

            // Invoke the method
            Object result = method.invoke(controller, args);
            
            // Handle the response
            if (result instanceof ResponseEntity) {
                ((ResponseEntity<?>) result).write(req, resp);
            } else {
                resp.setContentType("application/json");
                resp.getWriter().write(JsonUtil.toJson(result));
            }
            
            return result;
        } catch (Exception e) {
            e.printStackTrace();
            throw new ServletException("Failed to invoke endpoint method", e);
        }
    }

    public Method getMethod() {
        return method;
    }

    public Parameter[] getParameters() {
        return method.getParameters();
    }
    
    /**
     * Check if the given request URI matches the path pattern
     * @param requestUri The request URI to check
     * @return true if the URI matches the pattern, false otherwise
     */
    public boolean matchesPath(String requestUri) {
        Matcher matcher = pathPattern.matcher(requestUri);
        boolean matches = matcher.matches();
        if (matches) {
            // Log the captured path variables
            for (String varName : pathVariableNames) {
                System.out.println("  Path variable '" + varName + "': " + matcher.group(varName));
            }
        }
        return matches;
    }
} 