package sh.fyz.fiber;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.config.FiberConfig;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.challenge.internal.ChallengeController;
import sh.fyz.fiber.core.email.EmailService;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.core.security.csrf.CsrfController;
import sh.fyz.fiber.core.security.logging.AuditLogService;
import sh.fyz.fiber.docs.DocumentationController;
import sh.fyz.fiber.handler.FiberErrorHandler;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.middleware.impl.CsrfMiddleware;
import sh.fyz.fiber.validation.ValidationInitializer;
import sh.fyz.fiber.handler.RouterServlet;
import sh.fyz.fiber.core.security.filters.SecurityHeadersFilter;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2AuthenticationService;
import sh.fyz.fiber.handler.parameter.ParameterHandlerRegistry;
import sh.fyz.fiber.core.authentication.RoleRegistry;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class FiberServer {

    private boolean isDev = false;
    private int port = -1;
    private FiberConfig config;
    private AuthenticationService<?> authService;
    private OAuth2AuthenticationService<?> oauthService;
    private OAuth2ClientService oauthClientService;
    private EmailService emailService;
    private AuditLogService auditLogService;
    private CorsService corsService;
    private static FiberServer instance;
    private final Server server;
    private final ServletContextHandler context;
    private final List<Middleware> globalMiddleware;
    private final EndpointRegistry endpointRegistry;
    private final DocumentationController documentationController;
    private boolean documentationEnabled;
    private final RoleRegistry roleRegistry;
    private final ChallengeRegistry challengeRegistry;

    public FiberConfig getConfig() {
        return config;
    }

    public FiberServer(int port) {
        this(port, false);
    }

    public FiberServer(int port, boolean enableDocumentation) {
        instance = this;
        this.config = new FiberConfig();
        this.port = port;
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

        if (enableDocumentation) enableDocumentation();

        //Default value
        registerController(new ChallengeController());
    }

    public void enableDevelopmentMode() {
        System.out.println("Development mode enabled");
        this.isDev = true;
    }

    public boolean isDev() {
        return isDev;
    }

    public static FiberServer get() {
        if (instance == null) {
            throw new IllegalStateException("FiberServer has not been initialized");
        }
        return instance;
    }

    public void setCorsService(CorsService corsService) {
        System.out.println("Setting CORS service: " + corsService);
        this.corsService = corsService;
    }

    public CorsService getCorsService() {
        if (corsService == null) {
            //Set default config if none is present
            this.corsService = new CorsService()
                    .addAllowedOrigin("http://localhost:"+port)
                    .addAllowedOrigin("http://127.0.0.1:"+port)
                    .addAllowedOrigin("http://localhost:3000")
                    .addAllowedOrigin("http://127.0.0.1:3000")
                    .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"))
                    .setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"))
                    .setAllowCredentials(true)
                    .setMaxAge(3600);
        }
        return corsService;
    }

    public void setOauthClientService(OAuth2ClientService oauthClientService) {
        this.oauthClientService = oauthClientService;
    }

    public OAuth2ClientService getOauthClientService() {
        if (oauthClientService == null) {
            throw new IllegalStateException("OAuth2ClientService has not been set");
        }
        return oauthClientService;
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

    public ChallengeRegistry getChallengeRegistry() {
        return challengeRegistry;
    }

    public void setAuditLogService(AuditLogService auditLogService) {
        this.auditLogService = auditLogService;
    }

    public AuditLogService getAuditLogService() {
        return auditLogService;
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

    public void enableCSRFProtection() {
        addMiddleware(new CsrfMiddleware());
        registerController(new CsrfController());
    }

    private void enableDocumentation() {
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

    /**
     * Set the server header that will be sent in HTTP responses
     * @param header The server header value to use
     */
    public void setServerHeader(String header) {
        sh.fyz.fiber.core.security.filters.SecurityHeadersFilter.setServerHeader(header);
    }

    public void start() throws Exception {
        // Create a single servlet to handle all endpoints
        ServletHolder holder = new ServletHolder(new RouterServlet(endpointRegistry));
        context.addServlet(holder, "/*");
        context.setErrorHandler(new FiberErrorHandler());
        server.start();
        System.out.println("WARNING: This server is running in development mode. Do not use in production.");
    }

    public void stop() throws Exception {
        server.stop();
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public EmailService getEmailService() {
        return emailService;
    }
} 