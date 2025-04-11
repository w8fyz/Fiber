package sh.fyz.fiber.example;

import sh.fyz.architect.Architect;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.fiber.FiberServer;

public class Main {

    public static UserRepository userRepository;
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
        FiberServer server = new FiberServer(8080);
        
        // Enable API documentation
        server.enableDocumentation();
        
        // Initialize repositories and services
        userRepository = new UserRepository();
        ImplAuthService authService = new ImplAuthService(userRepository);
        OAuthService oauthService = new OAuthService(authService, userRepository);
        
        // Register OAuth providers
        oauthService.registerProvider(new DiscordProvider("882758522725613638", "WOjENOu6dJUef3_kNpKrcNxLg2CHvZac"));
        
        // Set services in the server
        server.setAuthService(authService);
        server.setOAuthService(oauthService);
        
        // Initialize roles and permissions using class-based roles
        server.getRoleRegistry().registerRoleClasses(
            UserRole.class
        );
        
        // Register controllers with dependencies
        server.registerController(new ExampleController());
        server.registerController(new AuthController(oauthService));
        server.registerController(new RoleExampleController());
        
        // Start the server
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.println("API Documentation available at http://localhost:8080/docs/ui");
        System.out.println("Raw API Documentation available at http://localhost:8080/docs/api");
    }
} 