package sh.fyz.fiber.core;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.security.Permission;
import sh.fyz.fiber.annotations.security.RequireRole;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.middleware.Middleware;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EndpointRegistry {

    private static final Logger logger = LoggerFactory.getLogger(EndpointRegistry.class);

    private final Map<String, EndpointHandler> endpoints;
    private final List<Middleware> globalMiddleware;
    private final PathTrie<EndpointHandler> dynamicTrie;
    private String[] defaultRoles;

    public EndpointRegistry(List<Middleware> globalMiddleware) {
        this.endpoints = new ConcurrentHashMap<>();
        this.globalMiddleware = globalMiddleware;
        this.defaultRoles = new String[0];
        this.dynamicTrie = new PathTrie<>();
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

        for (Method method : controllerClass.getDeclaredMethods()) {
            RequestMapping mapping = method.getAnnotation(RequestMapping.class);
            if (mapping != null) {
                String path = EndpointHandler.normalizePath(basePath + mapping.value());
                registerEndpoint(path, mapping.method(), method, controllerInstance);
            }
        }
    }

    public void registerEndpoint(String path, RequestMapping.Method httpMethod, Method method, Object controllerInstance) {
        RequireRole methodRole = method.getAnnotation(RequireRole.class);
        String[] requiredRoles = methodRole != null ? methodRole.value() : defaultRoles;

        String key = path + ":" + httpMethod;
        EndpointHandler handler = new EndpointHandler(controllerInstance, method, globalMiddleware, requiredRoles);
        EndpointHandler previous = endpoints.putIfAbsent(key, handler);
        if (previous != null) {
            logger.warn("Duplicate endpoint registration ignored: {} {}", httpMethod, path);
            return;
        }

        if (path.contains("{") || path.contains("*")) {
            dynamicTrie.add(path, httpMethod.name(), handler);
        }

        // Warn at registration if the endpoint is open (no role/permission/default roles).
        boolean hasPermission = method.isAnnotationPresent(Permission.class);
        if ((requiredRoles == null || requiredRoles.length == 0) && !hasPermission) {
            logger.warn("[Fiber] Endpoint {} {} is registered without @RequireRole/@Permission and no defaultRoles is configured", httpMethod, path);
        }
    }

    /**
     * @return immutable view of all registered endpoints. Mutations must go through
     *         {@link #registerEndpoint(String, RequestMapping.Method, Method, Object)}.
     */
    public Map<String, EndpointHandler> getEndpoints() {
        return Collections.unmodifiableMap(endpoints);
    }

    /** Internal — accessor for the router's lookup. */
    public PathTrie<EndpointHandler> getDynamicTrie() {
        return dynamicTrie;
    }
}
