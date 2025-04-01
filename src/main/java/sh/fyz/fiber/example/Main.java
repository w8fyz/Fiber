package sh.fyz.fiber.example;

import sh.fyz.fiber.FiberServer;

public class Main {
    public static void main(String[] args) throws Exception {
        // Create server
        FiberServer server = new FiberServer(8080);
        
        // Enable API documentation
        server.enableDocumentation();
        
        // Register controllers
        server.registerController(ExampleController.class);
        
        // Start the server
        server.start();
        
        System.out.println("Server started on http://localhost:8080");
        System.out.println("API Documentation available at http://localhost:8080/docs/ui");
        System.out.println("Raw API Documentation available at http://localhost:8080/docs/api");
    }
} 