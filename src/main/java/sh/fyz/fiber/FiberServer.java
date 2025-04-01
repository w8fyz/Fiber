package sh.fyz.fiber;

import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.docs.DocumentationController;
import sh.fyz.fiber.handler.EndpointHandler;
import sh.fyz.fiber.middleware.Middleware;
import sh.fyz.fiber.validation.ValidationInitializer;

import java.lang.reflect.Method;
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

    public void registerController(Class<?> controllerClass) {
        endpointRegistry.registerController(controllerClass);
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
                ServletHolder apiHolder = new ServletHolder(
                    new EndpointHandler(documentationController, apiMethod, globalMiddleware)
                );
                context.addServlet(apiHolder, "/docs/api");
            }

            if (uiMethod != null) {
                ServletHolder uiHolder = new ServletHolder(
                    new EndpointHandler(documentationController, uiMethod, globalMiddleware)
                );
                context.addServlet(uiHolder, "/docs/ui");
            }

            if (cssMethod != null) {
                ServletHolder cssHolder = new ServletHolder(
                    new EndpointHandler(documentationController, cssMethod, globalMiddleware)
                );
                context.addServlet(cssHolder, "/docs/css/*");
            }

            if (jsMethod != null) {
                ServletHolder jsHolder = new ServletHolder(
                    new EndpointHandler(documentationController, jsMethod, globalMiddleware)
                );
                context.addServlet(jsHolder, "/docs/js/*");
            }
        }
    }

    public void start() throws Exception {
        // Register all endpoints as servlets
        endpointRegistry.getEndpoints().forEach((path, handler) -> {
            String endpointPath = path.split(":")[0];
            ServletHolder holder = new ServletHolder(handler);
            context.addServlet(holder, endpointPath);
        });
        
        server.start();
    }

    public void stop() throws Exception {
        server.stop();
    }
} 