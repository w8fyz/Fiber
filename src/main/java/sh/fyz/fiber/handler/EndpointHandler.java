package sh.fyz.fiber.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.security.Permission;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.util.JsonUtil;
import sh.fyz.fiber.handler.parameter.ParameterHandler;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;
import sh.fyz.fiber.core.authentication.RoleRegistry;
import sh.fyz.fiber.core.authentication.entities.Role;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public Object handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        System.out.println("Handling request for method: " + method.getName());
        // Log request details

        // Check if method requires authentication (either through roles, permissions, or @AuthenticatedUser)
        boolean requiresAuth = (requiredRoles != null && requiredRoles.length > 0) || 
                              (requiredPermissions != null && requiredPermissions.length > 0);
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

            UserAuth user = AuthMiddleware.getCurrentUser(req);
            if (user == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_UNAUTHORIZED, "Authentication required");
                return null;
            }

            String userRole = user.getRole();
            RoleRegistry roleRegistry = FiberServer.get().getRoleRegistry();
            Role role = roleRegistry.getRole(userRole);
            
            if (role == null) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "User has no valid role");
                return null;
            }
            
            // Check if user has required role
            if (requiredRoles != null && requiredRoles.length > 0) {
                boolean hasRequiredRole = Arrays.stream(requiredRoles)
                    .anyMatch(required -> role.getIdentifier().equals(required));
                
                if (!hasRequiredRole) {
                    ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Insufficient role");
                    return null;
                }
            }
            
            // Check if user has required permissions
            if (requiredPermissions != null && requiredPermissions.length > 0) {
                boolean hasRequiredPermissions = Arrays.stream(requiredPermissions)
                    .allMatch(permission -> role.hasPermission(permission));
                
                if (!hasRequiredPermissions) {
                    ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_FORBIDDEN, "Insufficient permissions");
                    return null;
                }
            }
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