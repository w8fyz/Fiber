package sh.fyz.fiber.example;

import sh.fyz.architect.Architect;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;
import sh.fyz.fiber.core.challenge.impl.EmailVerificationChallenge;
import sh.fyz.fiber.core.email.EmailService;
import sh.fyz.fiber.core.security.cors.CorsService;
import sh.fyz.fiber.example.controller.AuthController;
import sh.fyz.fiber.example.controller.ExampleController;
import sh.fyz.fiber.example.oauth2.providers.DiscordProvider;
import sh.fyz.fiber.example.repo.Oauth2ClientRepository;
import sh.fyz.fiber.example.repo.UserRepository;
import sh.fyz.fiber.example.repo.entities.UserRole;

import java.util.Arrays;

public class Main {

    public static UserRepository userRepository;
    public static Oauth2ClientRepository oauth2clientRepository;
    public static void main(String[] args) throws Exception {

        Architect architect = new Architect()
                .setDatabaseCredentials(
                        new DatabaseCredentials(
                                new PostgreSQLAuth(
                                        "localhost",
                                        5432, "fiber"),
                                "postgres" ,"75395185Aa===", 16));
        architect.start();

        // Create server
        FiberServer server = new FiberServer(9090, true);
        server.enableDevelopmentMode();
        // Initialize repositories and services
        userRepository = new UserRepository();
        oauth2clientRepository = new Oauth2ClientRepository();
        ImplAuthService authService = new ImplAuthService(userRepository);
        OAuthService oauthService = new OAuthService(authService, userRepository);
        
        // Register OAuth providers
        oauthService.registerProvider(new DiscordProvider("882758522725613638", "WOjENOu6dJUef3_kNpKrcNxLg2CHvZac"));
        
        // Set services in the server
        server.setAuthService(authService);
        server.setOAuthService(oauthService);
        server.setAuditLogService(new LogService());
        server.enableCSRFProtection();
        server.setEmailService(new EmailService(
                "smtp.sendgrid.net",
                "thibeau.benet@freshperf.fr",
                465,
                "apikey","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",
                true,
                true
        ));

        server.setCorsService(new CorsService()
                .addAllowedOrigin("http://localhost:3000")
                .addAllowedOrigin("http://127.0.0.1:3000")
                .addAllowedOrigin("https://85zg9d.csb.app")
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
        server.setOauthClientService(new OAuth2ClientService(oauth2clientRepository));
        
        // Initialize roles and permissions using class-based roles
        server.getRoleRegistry().registerRoleClasses(
            UserRole.class
        );

        server.getChallengeRegistry().registerChallengeType("EMAIL_VERIFICATION", EmailVerificationChallenge::create);
        
        // Register controllers with dependencies
        server.registerController(new ExampleController());
        server.registerController(new AuthController(oauthService));
        
        // Start the server
        server.start();
        
        System.out.println("Server started on http://localhost:9090");
        System.out.println("API Documentation available at http://localhost:9090/docs/ui");
        System.out.println("Raw API Documentation available at http://localhost:9090/docs/api");
    }
} 