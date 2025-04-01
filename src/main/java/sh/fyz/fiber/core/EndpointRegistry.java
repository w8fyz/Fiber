package sh.fyz.fiber.core;

import sh.fyz.fiber.annotations.Controller;
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

    public EndpointRegistry(List<Middleware> globalMiddleware) {
        this.endpoints = new HashMap<>();
        this.globalMiddleware = globalMiddleware;
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

        for (Method method : controllerClass.getDeclaredMethods()) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                String path = basePath + mapping.value();
                RequestMapping.Method httpMethod = mapping.method();
                
                endpoints.put(path + ":" + httpMethod, new EndpointHandler(controllerInstance, method, globalMiddleware));
            }
        }
    }

    public Map<String, EndpointHandler> getEndpoints() {
        return endpoints;
    }
} 