package sh.fyz.fiber.core;

import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.RequireRole;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.middleware.Middleware;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EndpointRegistry {
    private final Map<String, EndpointHandler> endpoints;
    private final List<Middleware> globalMiddleware;
    private String[] defaultRoles;

    public EndpointRegistry(List<Middleware> globalMiddleware) {
        this.endpoints = new HashMap<>();
        this.globalMiddleware = globalMiddleware;
        this.defaultRoles = new String[0];
    }

    public void setDefaultRoles(String[] roles) {
        this.defaultRoles = roles;
    }

    public void registerController(Class<?> controllerClass) {
        Controller controllerAnnotation = controllerClass.getAnnotation(Controller.class);
        if (controllerAnnotation == null) {
            throw new IllegalArgumentException("Class must be annotated with @Controller");
        }

        String basePath = controllerAnnotation.value();
        Object controllerInstance;
        try {
            controllerInstance = controllerClass.getDeclaredConstructor().newInstance();
        } catch (Exception e) {
            throw new RuntimeException("Failed to create controller instance", e);
        }

        // Get all methods declared in this class
        for (Method method : controllerClass.getDeclaredMethods()) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                String path = basePath + mapping.value();
                registerEndpoint(path, mapping.method(), method, controllerInstance);
            }
        }
    }

    public void registerEndpoint(String path, RequestMapping.Method httpMethod, Method method, Object controllerInstance) {
        // Check for method-level role requirements
        RequireRole methodRole = method.getAnnotation(RequireRole.class);
        String[] requiredRoles = methodRole != null ? methodRole.value() : defaultRoles;
        
        // Use a composite key of path and HTTP method
        String key = path + ":" + httpMethod;
        if (endpoints.containsKey(key)) {
            System.out.println("Endpoint already registered: " + key);
            return; // Skip if endpoint is already registered
        }
        System.out.println("Registering endpoint: " + key);
        System.out.println("  Controller: " + controllerInstance.getClass().getName());
        System.out.println("  Method: " + method.getName());
        System.out.println("  Path: " + path);
        System.out.println("  HTTP Method: " + httpMethod);
        endpoints.put(key, new EndpointHandler(controllerInstance, method, globalMiddleware, requiredRoles));
    }

    public Map<String, EndpointHandler> getEndpoints() {
        return endpoints;
    }
} 