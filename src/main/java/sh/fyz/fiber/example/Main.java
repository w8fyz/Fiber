package sh.fyz.fiber.example;

import sh.fyz.architect.Architect;
import sh.fyz.architect.cache.RedisCredentials;
import sh.fyz.architect.persistant.DatabaseCredentials;
import sh.fyz.architect.persistant.sql.provider.PostgreSQLAuth;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.core.authentication.AuthenticationService;

public class Main {

    public static UserRepository userRepository;
    public static void main(String[] args) throws Exception {

        Architect architect = new Architect().setRedisCredentials(
                new RedisCredentials("host.docker.internal", "1234", 6379, 10, 10))
                .setReceiver(true)
                .setDatabaseCredentials(
                        new DatabaseCredentials(
                                new PostgreSQLAuth(
                                        "host.docker.internal",
                                        5432, "fiber"),
                                "postgres" ,"75395185Aa===", 16));
        architect.start();

        // Create server
        FiberServer server = new FiberServer(8080);
        
        // Enable API documentation
        server.enableDocumentation();
        
        // Register controllers
        server.registerController(ExampleController.class);
        server.registerController(AuthController.class);
        userRepository = new UserRepository();

        ImplAuthService authService = new ImplAuthService(userRepository);
        server.setOAuthService(new OAuthService(authService, userRepository));
        server.setAuthService(new ImplAuthService(userRepository));

        // Start the server
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.println("API Documentation available at http://localhost:8080/docs/ui");
        System.out.println("Raw API Documentation available at http://localhost:8080/docs/api");
    }
} 