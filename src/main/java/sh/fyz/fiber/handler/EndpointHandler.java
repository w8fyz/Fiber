package sh.fyz.fiber.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.AuthenticatedUser;
import sh.fyz.fiber.annotations.Param;
import sh.fyz.fiber.annotations.PathVariable;
import sh.fyz.fiber.annotations.RequestBody;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class EndpointHandler extends HttpServlet {
    private final Object controller;
    private final Method method;
    private final List<Middleware> globalMiddleware;
    private final ObjectMapper objectMapper;
    private final String[] requiredRoles;
    private final Pattern pathPattern;
    private String[] pathVariableNames;

    public EndpointHandler(Object controller, Method method, List<Middleware> globalMiddleware, String[] requiredRoles) {
        this.controller = controller;
        this.method = method;
        this.globalMiddleware = globalMiddleware;
        this.objectMapper = new ObjectMapper();
        this.requiredRoles = requiredRoles;

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
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.GET) {
                handleRequest(req, resp);
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.POST) {
                handleRequest(req, resp);
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doPut(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.PUT) {
                handleRequest(req, resp);
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    @Override
    protected void doDelete(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        if (method.isAnnotationPresent(RequestMapping.class)) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping.method() == RequestMapping.Method.DELETE) {
                handleRequest(req, resp);
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_METHOD_NOT_ALLOWED, "Method not allowed");
            }
        }
    }

    public Object handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        // Log request details
        System.out.println("Handling request: " + req.getMethod() + " " + req.getRequestURI());
        System.out.println("Path pattern: " + pathPattern.pattern());
        System.out.println("Path variables: " + String.join(", ", pathVariableNames));

        // Check if method requires authentication (either through roles or @AuthenticatedUser)
        boolean requiresAuth = requiredRoles != null && requiredRoles.length > 0;
        for (Parameter parameter : method.getParameters()) {
            if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                requiresAuth = true;
                break;
            }
        }

        // Process authentication if required
        if (requiresAuth) {
            if (!AuthMiddleware.process(req, resp)) {
                return null;
            }

            // Check if user has required role
            if (requiredRoles != null && requiredRoles.length > 0) {
                String userRole = AuthMiddleware.getCurrentUserRole(req);
                boolean hasRequiredRole = false;
                for (String role : requiredRoles) {
                    if (role.equals(userRole)) {
                        hasRequiredRole = true;
                        break;
                    }
                }

                if (!hasRequiredRole) {
                    ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
                    return null;
                }
            }
        }

        // Execute global middleware
        for (Middleware middleware : globalMiddleware) {
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
                        ErrorResponse.send(resp, path, HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
                    }
                    return null;
                }
            }

            // Invoke the method
            Object result = method.invoke(controller, args);
            
            // Handle the response
            if (result instanceof ResponseEntity) {
                ((ResponseEntity<?>) result).write(resp);
            } else {
                resp.setContentType("application/json");
                resp.getWriter().write(JsonUtil.toJson(result));
            }
            
            return result;
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
    
    /**
     * Check if the given request URI matches the path pattern
     * @param requestUri The request URI to check
     * @return true if the URI matches the pattern, false otherwise
     */
    public boolean matchesPath(String requestUri) {
        System.out.println("Checking if path matches: " + requestUri);
        System.out.println("  Pattern: " + pathPattern.pattern());
        Matcher matcher = pathPattern.matcher(requestUri);
        boolean matches = matcher.matches();
        System.out.println("  Matches: " + matches);
        if (matches) {
            // Log the captured path variables
            for (String varName : pathVariableNames) {
                System.out.println("  Path variable '" + varName + "': " + matcher.group(varName));
            }
        }
        return matches;
    }
} 