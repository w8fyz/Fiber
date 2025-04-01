package sh.fyz.fiber;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sh.fyz.fiber.annotations.AuthenticatedUser;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.annotations.RequireRole;
import sh.fyz.fiber.core.AuthMiddleware;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.UserAuth;
import sh.fyz.fiber.docs.DocumentationController;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationInitializer;
import sh.fyz.fiber.handler.RouterServlet;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class FiberServer {
    private final Server server;
    private final ServletContextHandler context;
    private final List<Middleware> globalMiddleware;
    private final EndpointRegistry endpointRegistry;
    private final DocumentationController documentationController;
    private boolean documentationEnabled;

    public FiberServer(int port) {
        this.server = new Server(port);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.globalMiddleware = new ArrayList<>();
        this.endpointRegistry = new EndpointRegistry(globalMiddleware);
        this.documentationController = new DocumentationController();
        this.documentationEnabled = false;

        // Initialize validation system
        ValidationInitializer.initialize();

        // Set up server
        server.setHandler(context);
    }

    /**
     * Register a controller class
     * @param controllerClass The controller class to register
     */
    public void registerController(Class<?> controllerClass) {
        endpointRegistry.registerController(controllerClass);
        if (documentationEnabled) {
            documentationController.registerController(controllerClass);
        }
    }

    /**
     * Register a controller instance
     * @param controller The controller instance to register
     */
    public void registerController(Object controller) {
        Class<?> controllerClass = controller.getClass();
        Controller annotation = controllerClass.getAnnotation(Controller.class);
        if (annotation == null) {
            throw new IllegalArgumentException("Controller class must be annotated with @Controller");
        }

        String basePath = annotation.value();
        if (!basePath.startsWith("/")) {
            basePath = "/" + basePath;
        }

        // Register all endpoints in the controller
        for (Method method : controllerClass.getMethods()) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String path = requestMapping.value();
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                String fullPath = basePath + path;
                endpointRegistry.registerEndpoint(fullPath, requestMapping.method(), method, controller);
            }
        }

        if (documentationEnabled) {
            documentationController.registerController(controllerClass);
        }
    }

    public void addMiddleware(Middleware middleware) {
        globalMiddleware.add(middleware);
    }

    public void enableDocumentation() {
        if (!documentationEnabled) {
            documentationEnabled = true;
            
            // Register API documentation endpoint
            Method apiMethod = null;
            Method uiMethod = null;
            Method cssMethod = null;
            Method jsMethod = null;
            
            for (Method method : DocumentationController.class.getDeclaredMethods()) {
                switch (method.getName()) {
                    case "getApiDocs" -> apiMethod = method;
                    case "getSwaggerUI" -> uiMethod = method;
                    case "getCSS" -> cssMethod = method;
                    case "getJavaScript" -> jsMethod = method;
                }
            }

            if (apiMethod != null) {
                endpointRegistry.registerEndpoint("/docs/api", RequestMapping.Method.GET, apiMethod, documentationController);
            }

            if (uiMethod != null) {
                endpointRegistry.registerEndpoint("/docs/ui", RequestMapping.Method.GET, uiMethod, documentationController);
            }

            if (cssMethod != null) {
                endpointRegistry.registerEndpoint("/docs/css/*", RequestMapping.Method.GET, cssMethod, documentationController);
            }

            if (jsMethod != null) {
                endpointRegistry.registerEndpoint("/docs/js/*", RequestMapping.Method.GET, jsMethod, documentationController);
            }
        }
    }

    public void start() throws Exception {
        // Create a single servlet to handle all endpoints
        ServletHolder holder = new ServletHolder(new RouterServlet(endpointRegistry));
        context.addServlet(holder, "/*");
        
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }

    /**
     * Process method parameters and inject authenticated user if requested
     */
    public static Object[] processParameters(Method method, HttpServletRequest request) {
        Parameter[] parameters = method.getParameters();
        Object[] args = new Object[parameters.length];

        for (int i = 0; i < parameters.length; i++) {
            Parameter param = parameters[i];
            if (param.isAnnotationPresent(AuthenticatedUser.class)) {
                if (UserAuth.class.isAssignableFrom(param.getType())) {
                    // Create a UserAuth instance from the request attributes
                    String id = AuthMiddleware.getCurrentUserId(request);
                    String username = AuthMiddleware.getCurrentUsername(request);
                    String role = AuthMiddleware.getCurrentUserRole(request);
                    
                    // Create an anonymous implementation of UserAuth
                    args[i] = new UserAuth() {
                        @Override
                        public String getId() { return id; }
                        @Override
                        public String getUsername() { return username; }
                        @Override
                        public String getRole() { return role; }
                    };
                }
            }
        }

        return args;
    }
} 