package sh.fyz.fiber.example;

import sh.fyz.architect.Architect;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;
import sh.fyz.fiber.core.email.EmailService;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.example.controller.ExampleAuthController;
import sh.fyz.fiber.example.controller.ExampleController;
import sh.fyz.fiber.example.oauth2.providers.ExampleDiscordProvider;
import sh.fyz.fiber.example.repo.ExampleOauth2ClientRepository;
import sh.fyz.fiber.example.repo.ExampleUserRepository;
import sh.fyz.fiber.example.repo.entities.ExampleUserRole;
import sh.fyz.fiber.example.dashboard.ExampleAdminDashboard;

import java.util.Arrays;

public class ExampleMain {

    public static ExampleUserRepository exampleUserRepository;
    public static ExampleOauth2ClientRepository exampleOauth2ClientRepository;
    public static void main(String[] args) throws Exception {

        Architect architect = new Architect()
                .setDatabaseCredentials(
                        new DatabaseCredentials(
                                new PostgreSQLAuth(
                                        "localhost",
                                        5432, "fiber"),
                                "postgres" ,"", 16, 8));
        architect.start();

        // Create server
        FiberServer server = new FiberServer(9090, true);
        server.enableDevelopmentMode();
        // Initialize repositories and services
        exampleUserRepository = new ExampleUserRepository();
        exampleOauth2ClientRepository = new ExampleOauth2ClientRepository();
        ExampleImplAuthService authService = new ExampleImplAuthService(exampleUserRepository);
        ExampleOAuthService oauthServiceExample = new ExampleOAuthService(authService, exampleUserRepository);
        
        // Register OAuth providers
        oauthServiceExample.registerProvider(new ExampleDiscordProvider("", ""));
        
        // Set services in the server
        server.setAuthService(authService);
        server.setOAuthService(oauthServiceExample);
        server.setAuditLogService(new ExampleLogService());
        server.enableCSRFProtection();
        server.setEmailService(new EmailService(
                "smtp.sendgrid.net",
                "thibeau.benet@freshperf.fr",
                465,
                "apikey","",
                true,
                true
        ));

        server.setCorsService(new CorsService()
                .addAllowedOrigin("http://localhost:3000")
                .addAllowedOrigin("http://127.0.0.1:3000")
                .addAllowedOrigin("https://")
                .setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"))
                .setAllowedHeaders(Arrays.asList(
                    "Content-Type",
                    "Authorization",
                    "X-CSRF-TOKEN",
                    "X-Requested-With",
                    "Accept",
                    "Origin"
                ))
                .setAllowCredentials(true)
                .setMaxAge(3600));
        server.setOauthClientService(new OAuth2ClientService(exampleOauth2ClientRepository));
        
        // Initialize roles and permissions using class-based roles
        server.getRoleRegistry().registerRoleClasses(
            ExampleUserRole.class
        );

        // Register controllers with dependencies
        server.registerController(new ExampleController());
        server.registerController(new ExampleAuthController(oauthServiceExample));
        server.registerController(new ExampleTest2Controller());
        server.getDashboardRegistry().register(new ExampleAdminDashboard());
        
        // Start the server
        server.start();
        
        //System.out.println("Server started on http://localhost:9090");
        //System.out.println("API Documentation available at http://localhost:9090/docs/ui");
        //System.out.println("Raw API Documentation available at http://localhost:9090/docs/api");
    }
} 