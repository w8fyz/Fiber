package sh.fyz.fiber;

import jakarta.servlet.http.HttpServletRequest;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sh.fyz.fiber.annotations.AuthenticatedUser;
import sh.fyz.fiber.annotations.Controller;
import sh.fyz.fiber.annotations.RequestMapping;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.docs.DocumentationController;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationInitializer;
import sh.fyz.fiber.handler.RouterServlet;
import sh.fyz.fiber.core.security.filters.SecurityHeadersFilter;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;
import sh.fyz.fiber.core.authentication.RoleRegistry;
import sh.fyz.fiber.core.authentication.OpenIDAuthenticationService;

import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.util.ArrayList;
import java.util.List;

public class FiberServer {

    private AuthenticationService<?> authService;
    private OAuth2AuthenticationService<?> oauthService;
    private OpenIDAuthenticationService<?> openIDService;
    private static FiberServer instance;
    private final Server server;
    private final ServletContextHandler context;
    private final List<Middleware> globalMiddleware;
    private final EndpointRegistry endpointRegistry;
    private final DocumentationController documentationController;
    private boolean documentationEnabled;
    private final RoleRegistry roleRegistry;
    private final ChallengeRegistry challengeRegistry;

    public FiberServer(int port) {
        instance = this;
        this.server = new Server(port);
        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.globalMiddleware = new ArrayList<>();
        this.endpointRegistry = new EndpointRegistry(globalMiddleware);
        this.documentationController = new DocumentationController();
        this.documentationEnabled = false;
        this.roleRegistry = new RoleRegistry();
        this.challengeRegistry = new ChallengeRegistry();

        // Initialize validation system
        ValidationInitializer.initialize();

        // Initialize parameter handlers
        ParameterHandlerRegistry.initialize();

        // Set up server
        server.setHandler(context);
        
        // Register security filter
        context.addFilter(SecurityHeadersFilter.class, "/*", null);
    }

    public static FiberServer get() {
        if (instance == null) {
            throw new IllegalStateException("FiberServer has not been initialized");
        }
        return instance;
    }

    public void setAuthService(AuthenticationService<?> authService) {
        this.authService = authService;
    }

    public AuthenticationService<?> getAuthService() {
        if (authService == null) {
            throw new IllegalStateException("AuthenticationService has not been set");
        }
        return authService;
    }
    
    public void setOAuthService(OAuth2AuthenticationService<?> oauthService) {
        this.oauthService = oauthService;
    }
    
    public OAuth2AuthenticationService<?> getOAuthService() {
        if (oauthService == null) {
            throw new IllegalStateException("OAuth2AuthenticationService has not been set");
        }
        return oauthService;
    }

    public void setOpenIDService(OpenIDAuthenticationService<?> openIDService) {
        this.openIDService = openIDService;
    }

    public OpenIDAuthenticationService<?> getOpenIDService() {
        if (openIDService == null) {
            throw new IllegalStateException("OpenIDAuthenticationService has not been set");
        }
        return openIDService;
    }

    /**
     * Get the role registry for managing roles and permissions
     */
    public RoleRegistry getRoleRegistry() {
        return roleRegistry;
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
        for (Method method : controllerClass.getDeclaredMethods()) {
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
                    case "getUI" -> uiMethod = method;
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
} 