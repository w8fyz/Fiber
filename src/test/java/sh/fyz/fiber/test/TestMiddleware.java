package sh.fyz.fiber.test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.middleware.Middleware;

import java.io.IOException;

public class TestMiddleware implements Middleware {

    @Override
    public int priority() {
        return 10;
    }

    @Override
    public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        request.setAttribute("test-middleware", "hello-from-middleware");
        response.setHeader("X-Test-Middleware-Ran", "true");
        return true;
    }
}
