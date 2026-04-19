package sh.fyz.fiber;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.thread.QueuedThreadPool;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.config.FiberConfig;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;
import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeCallback;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;
import sh.fyz.fiber.core.challenge.internal.ChallengeController;
import sh.fyz.fiber.core.dto.DTOConvertible;
import sh.fyz.fiber.core.email.EmailService;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.core.security.csrf.CsrfController;
import sh.fyz.fiber.core.security.logging.AuditLogService;
import sh.fyz.fiber.core.upload.FileUploadManager;
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
import sh.fyz.fiber.core.authentication.AuthResolver;
import sh.fyz.fiber.core.authentication.impl.BasicAuthenticator;
import sh.fyz.fiber.core.session.SessionService;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.util.HttpUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.github.classgraph.ClassGraph;
import io.github.classgraph.ScanResult;

import java.lang.reflect.Method;
import java.net.InetAddress;
import java.util.*;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FiberServer {

    private static final Logger logger = LoggerFactory.getLogger(FiberServer.class);

    private boolean isDev = false;
    private int port = -1;
    private FiberConfig config;
    private AuthenticationService<?> authService;
    private OAuth2AuthenticationService<?> oauthService;
    private OAuth2ClientService oauthClientService;
    private EmailService emailService;
    private AuditLogService auditLogService;
    private static volatile FiberServer instance;
    private final Server server;
    private final ServletContextHandler context;
    private final List<Middleware> globalMiddleware;
    private final EndpointRegistry endpointRegistry;
    private final DocumentationController documentationController;
    private boolean documentationEnabled;
    private final RoleRegistry roleRegistry;
    private final ChallengeRegistry challengeRegistry;
    private final AuthResolver authResolver;
    private CsrfMiddleware csrfMiddleware;
    private CorsService corsService;
    private final BasicAuthenticator basicAuthenticator;
    private SessionService sessionService;
    private boolean challengeControllerRegistered = false;
    private long maxFileSize = 50_000_000;
    private long maxRequestSize = 100_000_000;
    private int fileSizeThreshold = 1_000_000;
    private boolean started = false;
    private final ScheduledExecutorService sharedExecutor;

    public FiberConfig getConfig() {
        return config;
    }

    public FiberServer(int port) {
        this(port, false);
    }

    public FiberServer(int port, boolean enableDocumentation) {
        this.config = new FiberConfig();
        this.port = port;
        this.sharedExecutor = Executors.newScheduledThreadPool(2, r -> {
            Thread t = Thread.ofVirtual().name("fiber-shared-").unstarted(r);
            t.setDaemon(true);
            return t;
        });

        // P-08/P-09: Configure Jetty with virtual threads (Java 21)
        QueuedThreadPool threadPool = new QueuedThreadPool();
        threadPool.setVirtualThreadsExecutor(Executors.newVirtualThreadPerTaskExecutor());
        this.server = new Server(threadPool);
        org.eclipse.jetty.server.ServerConnector connector = new org.eclipse.jetty.server.ServerConnector(server);
        connector.setPort(port);
        server.addConnector(connector);

        this.context = new ServletContextHandler(ServletContextHandler.SESSIONS);
        this.globalMiddleware = new CopyOnWriteArrayList<>();
        this.endpointRegistry = new EndpointRegistry(globalMiddleware);
        this.documentationController = new DocumentationController();
        this.documentationEnabled = false;
        this.roleRegistry = new RoleRegistry();
        this.challengeRegistry = new ChallengeRegistry();
        this.authResolver = new AuthResolver();
        this.authResolver.registerAuthenticator(new sh.fyz.fiber.core.authentication.impl.CookieAuthenticator());
        this.authResolver.registerAuthenticator(new sh.fyz.fiber.core.authentication.impl.BearerAuthenticator());
        this.corsService = new CorsService();
        this.basicAuthenticator = new BasicAuthenticator();

        ValidationInitializer.initialize();
        ParameterHandlerRegistry.initialize();

        server.setHandler(context);
        context.addFilter(SecurityHeadersFilter.class, "/*", null);

        if (enableDocumentation) enableDocumentation();

        // Publish the fully-initialised instance last to avoid races where
        // FiberServer.get() is called from one of the components above before
        // its dependencies are wired.
        instance = this;
    }

    public void enableDevelopmentMode() {
        this.isDev = true;
    }

    public void preloadDto() {
        try (ScanResult scanResult = new ClassGraph()
                .enableAnnotationInfo()
                .scan()) {

            scanResult.getClassesImplementing(DTOConvertible.class.getName()).forEach(classInfo -> {
                DTOConvertible.getCachedFields(classInfo.loadClass());
            });
        }
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

    public FiberServer setTrustedProxies(List<String> proxies) {
        HttpUtil.setTrustedProxies(proxies == null ? null : new HashSet<>(proxies));
        return this;
    }

    public void setCorsService(CorsService corsService) {
        this.corsService = corsService;
    }

    public CorsService getCorsService() {
        if (corsService == null) {
            CorsService defaultCors = new CorsService()
                    .addAllowedOrigin("http://localhost:" + port)
                    .addAllowedOrigin("http://127.0.0.1:" + port)
                    .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE"))
                    .setAllowedHeaders(Arrays.asList("Content-Type", "Authorization"))
                    .setAllowCredentials(true)
                    .setMaxAge(3600);
            if (isDev) {
                defaultCors.addAllowedOrigin("http://localhost:3000");
                defaultCors.addAllowedOrigin("http://127.0.0.1:3000");
            }
            this.corsService = defaultCors;
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

    public Challenge registerChallenge(Challenge challenge, ChallengeCallback callback) {
        if (!challengeControllerRegistered) {
            registerController(new ChallengeController());
            challengeControllerRegistered = true;
        }
        return challengeRegistry.createChallenge(challenge, callback);
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

    public RoleRegistry getRoleRegistry() {
        return roleRegistry;
    }

    public void registerController(Class<?> controllerClass) {
        endpointRegistry.registerController(controllerClass);
        if (documentationEnabled) {
            documentationController.registerController(controllerClass);
        }
    }

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

        for (Method method : controllerClass.getDeclaredMethods()) {
            RequestMapping requestMapping = method.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String path = requestMapping.value();
                if (!path.startsWith("/")) {
                    path = "/" + path;
                }
                String fullPath = EndpointHandler.normalizePath(basePath + path);
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
        this.csrfMiddleware = new CsrfMiddleware();
        registerController(new CsrfController());
    }

    private void enableDocumentation() {
        if (!documentationEnabled) {
            documentationEnabled = true;

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

    public void setServerHeader(String header) {
        SecurityHeadersFilter.setServerHeader(header);
    }

    public void start() throws Exception {
        if (!isDev && config.isSecretAutoGenerated()) {
            throw new IllegalStateException(
                "[Fiber] JWT secret is required in production mode. " +
                "Set FIBER_SECRET_KEY env variable or configure JWT_SECRET_KEY in fiberconfig.json (min 32 chars). " +
                "Call enableDevelopmentMode() before start() for local development.");
        }

        if (isDev) {
            try {
                InetAddress bindAddr = InetAddress.getByName("0.0.0.0");
                if (!bindAddr.isLoopbackAddress()) {
                    logger.warn("[Fiber] Development mode is enabled on a non-loopback address. " +
                            "Security features (Secure cookies, strict SameSite) are relaxed. " +
                            "Do NOT use enableDevelopmentMode() in production.");
                }
            } catch (Exception ignored) {
                // Hostname resolution failure is not actionable here — the warning above
                // is purely informational and only emitted when bind != loopback.
            }
        }

        ServletHolder holder = new ServletHolder(new RouterServlet(endpointRegistry));
        holder.getRegistration().setMultipartConfig(
                new jakarta.servlet.MultipartConfigElement(
                        System.getProperty("java.io.tmpdir"),
                        maxFileSize, maxRequestSize, fileSizeThreshold
                )
        );
        context.addServlet(holder, "/*");
        context.setErrorHandler(new FiberErrorHandler());

        // Freeze role registrations so subsequent calls fail fast.
        roleRegistry.freeze();

        started = true;
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
        FileUploadManager.getInstance().shutdown();
        if (oauthClientService != null) {
            oauthClientService.shutdown();
        }
        sharedExecutor.shutdown();
        try {
            if (!sharedExecutor.awaitTermination(10, TimeUnit.SECONDS)) {
                logger.warn("[Fiber] Shared executor did not terminate within 10s — forcing shutdown");
                sharedExecutor.shutdownNow();
            }
        } catch (InterruptedException e) {
            sharedExecutor.shutdownNow();
            Thread.currentThread().interrupt();
        }
        started = false;
    }

    public void setEmailService(EmailService emailService) {
        this.emailService = emailService;
    }

    public EmailService getEmailService() {
        return emailService;
    }

    public AuthResolver getAuthResolver() {
        return authResolver;
    }

    public CsrfMiddleware getCsrfMiddleware() {
        return csrfMiddleware;
    }

    public BasicAuthenticator getBasicAuthenticator() {
        return basicAuthenticator;
    }

    public void setSessionService(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public SessionService getSessionService() {
        return sessionService;
    }

    public FiberServer setMaxFileSize(long maxFileSize) {
        this.maxFileSize = maxFileSize;
        return this;
    }

    public FiberServer setMaxRequestSize(long maxRequestSize) {
        this.maxRequestSize = maxRequestSize;
        return this;
    }

    public FiberServer setFileSizeThreshold(int fileSizeThreshold) {
        this.fileSizeThreshold = fileSizeThreshold;
        return this;
    }

    /**
     * Shared {@link ScheduledExecutorService} backed by virtual threads. Components that
     * need background scheduling (session expiration, OAuth code cleanup, upload purge)
     * should reuse this executor rather than spawning their own pools so that
     * {@link #stop()} can deterministically reclaim them.
     */
    public ScheduledExecutorService getSharedExecutor() {
        return sharedExecutor;
    }
}
