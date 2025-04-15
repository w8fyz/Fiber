package sh.fyz.fiber.core.security.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CorsService {
    private List<String> allowedOrigins = new ArrayList<>();
    private List<String> allowedMethods = Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS");
    private List<String> allowedHeaders = Arrays.asList("Content-Type", "Authorization");
    private boolean allowCredentials = true;
    private long maxAge = 3600;

    public CorsService() {
        // Par défaut, n'accepte aucune origine
    }

    public CorsService addAllowedOrigin(String origin) {
        this.allowedOrigins.add(origin);
        return this;
    }

    public CorsService setAllowedOrigins(List<String> origins) {
        this.allowedOrigins = new ArrayList<>(origins);
        return this;
    }

    public CorsService setAllowedMethods(List<String> methods) {
        this.allowedMethods = new ArrayList<>(methods);
        return this;
    }

    public CorsService setAllowedHeaders(List<String> headers) {
        this.allowedHeaders = new ArrayList<>(headers);
        return this;
    }

    public CorsService setAllowCredentials(boolean allowCredentials) {
        this.allowCredentials = allowCredentials;
        return this;
    }

    public CorsService setMaxAge(long maxAge) {
        this.maxAge = maxAge;
        return this;
    }

    public boolean isOriginAllowed(String origin) {
        if (allowedOrigins.isEmpty()) {
            return false;
        }
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    public void configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        
        if (origin != null && isOriginAllowed(origin)) {
            // Si credentials sont autorisés, on ne peut pas utiliser le wildcard "*"
            if (allowCredentials) {
                response.setHeader("Access-Control-Allow-Origin", origin);
                response.setHeader("Access-Control-Allow-Credentials", "true");
            } else {
                // Si pas de credentials, on peut utiliser le wildcard si configuré
                response.setHeader("Access-Control-Allow-Origin", 
                    allowedOrigins.contains("*") ? "*" : origin);
            }
            
            response.setHeader("Access-Control-Allow-Methods", String.join(", ", allowedMethods));
            response.setHeader("Access-Control-Allow-Headers", String.join(", ", allowedHeaders));
            response.setHeader("Access-Control-Max-Age", String.valueOf(maxAge));
        }
    }
} 