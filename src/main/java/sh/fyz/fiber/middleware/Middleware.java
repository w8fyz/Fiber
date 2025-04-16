package sh.fyz.fiber.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

/*
*   Define a middleware interface for handling HTTP requests and responses.
*   Executed in order of registration.
*   Use sh.fyz.fiber.core.Fiber.registerMiddleware() to register a middleware.
* */
public interface Middleware {

    int priority();

    boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
} 