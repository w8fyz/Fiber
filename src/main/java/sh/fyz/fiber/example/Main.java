package sh.fyz.fiber.example;

import sh.fyz.architect.Architect;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.challenge.ChallengeRegistry;

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
        FiberServer server = new FiberServer(8080, true);
        
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

        //server.getChallengeRegistry().registerChallengeType("CACA", ExampleChallenge::create);
        
        // Register controllers with dependencies
        server.registerController(new ExampleController());
        server.registerController(new AuthController(oauthService));
        
        // Start the server
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.println("API Documentation available at http://localhost:8080/docs/ui");
        System.out.println("Raw API Documentation available at http://localhost:8080/docs/api");
    }
} 