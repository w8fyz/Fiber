package sh.fyz.fiber.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.Param;
import sh.fyz.fiber.annotations.RequestBody;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.ErrorResponse;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationRegistry;
import sh.fyz.fiber.validation.ValidationResult;

import java.io.IOException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.List;

public class EndpointHandler extends HttpServlet {
    private final Object controller;
    private final Method method;
    private final List<Middleware> globalMiddleware;
    private final ObjectMapper objectMapper;

    public EndpointHandler(Object controller, Method method, List<Middleware> globalMiddleware) {
        this.controller = controller;
        this.method = method;
        this.globalMiddleware = globalMiddleware;
        this.objectMapper = new ObjectMapper();
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

    private void handleRequest(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
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

        for (int i = 0; i < parameters.length; i++) {
            Parameter parameter = parameters[i];
            Class<?> type = parameter.getType();

            if (type == HttpServletRequest.class) {
                args[i] = req;
            } else if (type == HttpServletResponse.class) {
                args[i] = resp;
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