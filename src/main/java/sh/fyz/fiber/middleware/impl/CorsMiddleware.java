package sh.fyz.fiber.middleware.impl;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.middleware.Middleware;

import java.io.IOException;

public class CorsMiddleware implements Middleware {
    @Override
    public boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException {
        FiberServer.get().getCorsService().configureCorsHeaders(request, response);
        return true;
    }
}
