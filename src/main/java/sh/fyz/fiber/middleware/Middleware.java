package sh.fyz.fiber.middleware;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

public interface Middleware {
    boolean handle(HttpServletRequest request, HttpServletResponse response) throws IOException;
} 