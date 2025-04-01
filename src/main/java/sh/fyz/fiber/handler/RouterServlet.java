package sh.fyz.fiber.handler;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.core.EndpointRegistry;
import sh.fyz.fiber.core.ErrorResponse;

import java.io.IOException;
import java.util.Map;
import java.util.regex.Pattern;

public class RouterServlet extends HttpServlet {
    private final EndpointRegistry endpointRegistry;

    public RouterServlet(EndpointRegistry endpointRegistry) {
        this.endpointRegistry = endpointRegistry;
    }

    @Override
    protected void service(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        String path = req.getRequestURI();
        String method = req.getMethod();
        
        EndpointHandler handler = findHandler(path, method);
        if (handler != null) {
            handler.handleRequest(req, resp);
        } else {
            ErrorResponse.send(resp, path, HttpServletResponse.SC_NOT_FOUND, "No handler found for path: " + path);
        }
    }

    private EndpointHandler findHandler(String path, String method) {
        Map<String, EndpointHandler> endpoints = endpointRegistry.getEndpoints();
        
        // First try exact match
        String key = path + ":" + method;
        EndpointHandler handler = endpoints.get(key);
        if (handler != null) {
            return handler;
        }

        // Then try pattern matching only for paths that contain wildcards or path variables
        for (Map.Entry<String, EndpointHandler> entry : endpoints.entrySet()) {
            String endpointKey = entry.getKey();
            String endpointPath = endpointKey.substring(0, endpointKey.lastIndexOf(':'));
            String endpointMethod = endpointKey.substring(endpointKey.lastIndexOf(':') + 1);
            
            // Only try pattern matching if the endpoint path contains wildcards or path variables
            if (endpointMethod.equals(method) && 
                (endpointPath.contains("*") || endpointPath.contains("{")) && 
                matchesPattern(path, endpointPath)) {
                return entry.getValue();
            }
        }
        
        return null;
    }

    private boolean matchesPattern(String path, String pattern) {
        // Convert the pattern to a regex
        String regex = pattern
            .replaceAll("\\*", ".*")  // Replace * with regex wildcard
            .replaceAll("\\{([^}]+)}", "([^/]+)"); // Replace {param} with capture group
        
        return Pattern.matches(regex, path);
    }
} 