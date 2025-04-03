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
import sh.fyz.fiber.core.AuthMiddleware;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.UserAuth;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

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

    public void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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
                return;
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
                    return;
                }
            }
        }

        // Execute global middleware
        for (Middleware middleware : globalMiddleware) {
            if (!middleware.handle(req, resp)) {
                return;
            }
        }

        try {
            Object[] args = prepareMethodArguments(req, resp);
            Object result = method.invoke(controller, args);
            
            // Handle response
            if (result instanceof ResponseEntity) {
                ((ResponseEntity<?>) result).write(resp);
            } else if (result != null) {
                resp.setContentType("application/json");
                objectMapper.writeValue(resp.getWriter(), result);
            }
            // If result is null and method is void, assume the method wrote directly to the response
        } catch (Exception e) {
            if (e instanceof IllegalArgumentException) {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_BAD_REQUEST, e.getMessage());
            } else {
                ErrorResponse.send(resp, req.getRequestURI(), HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Internal server error");
            }
        }
    }

    private Object[] prepareMethodArguments(HttpServletRequest req, HttpServletResponse resp) throws IOException {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        // Extract path variables
        String path = req.getRequestURI();
        Matcher matcher = pathPattern.matcher(path);
        if (!matcher.matches()) {
            throw new IllegalArgumentException("Path does not match pattern");
        }

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (type == HttpServletRequest.class) {
                args[i] = req;
            } else if (type == HttpServletResponse.class) {
                args[i] = resp;
            } else if (parameter.isAnnotationPresent(AuthenticatedUser.class)) {
                if (UserAuth.class.isAssignableFrom(type)) {
                    args[i] = AuthMiddleware.getCurrentUser(req);
                } else if(FiberServer.get().getAuthService().getUserClass().isAssignableFrom(type)) {
                    args[i] = FiberServer.get().getAuthService().getUserById(AuthMiddleware.getCurrentUserId(req));
                } else {
                    throw new IllegalArgumentException("Invalid type for @AuthenticatedUser: " + type);
                }
            } else if (parameter.isAnnotationPresent(PathVariable.class)) {
                PathVariable pathVar = parameter.getAnnotation(PathVariable.class);
                String value = matcher.group(pathVar.value());
                
                try {
                    Object convertedValue = convertValue(value, type);
                    ValidationResult result = ValidationRegistry.validateParameter(parameter, convertedValue);
                    if (!result.isValid()) {
                        throw new IllegalArgumentException(result.getFirstError());
                    }
                    args[i] = convertedValue;
                } catch (Exception e) {
                    if (e instanceof IllegalArgumentException) {
                        throw e;
                    }
                    throw new IllegalArgumentException("Invalid path variable format for: " + pathVar.value());
                }
            } else if (parameter.isAnnotationPresent(RequestBody.class)) {
                try {
                    // Read and validate request body
                    Object body = objectMapper.readValue(req.getReader(), type);
                    ValidationResult result = ValidationRegistry.validate(body);
                    if (!result.isValid()) {
                        throw new IllegalArgumentException(result.getFirstError());
                    }
                    args[i] = body;
                } catch (Exception e) {
                    if (e instanceof IllegalArgumentException) {
                        throw e;
                    }
                    throw new IllegalArgumentException("Invalid request body format");
                }
            } else if (parameter.isAnnotationPresent(Param.class)) {
                // Read and validate parameter
                Param param = parameter.getAnnotation(Param.class);
                String value = req.getParameter(param.value());
                
                try {
                    Object convertedValue = convertValue(value, type);
                    ValidationResult result = ValidationRegistry.validateParameter(parameter, convertedValue);
                    if (!result.isValid()) {
                        throw new IllegalArgumentException(result.getFirstError());
                    }
                    args[i] = convertedValue;
                } catch (Exception e) {
                    if (e instanceof IllegalArgumentException) {
                        throw e;
                    }
                    throw new IllegalArgumentException("Invalid parameter format for: " + param.value());
                }
            }
        }

        return args;
    }

    private Object convertValue(String value, Class<?> type) {
        if (value == null) {
            return null;
        }

        if (type == String.class) {
            return value;
        } else if (type == Integer.class || type == int.class) {
            return Integer.parseInt(value);
        } else if (type == Long.class || type == long.class) {
            return Long.parseLong(value);
        } else if (type == Double.class || type == double.class) {
            return Double.parseDouble(value);
        } else if (type == Boolean.class || type == boolean.class) {
            return Boolean.parseBoolean(value);
        } else {
            throw new IllegalArgumentException("Unsupported type: " + type);
        }
    }
} 