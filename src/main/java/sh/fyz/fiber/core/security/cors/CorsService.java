package sh.fyz.fiber.core.security.cors;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;

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

        if (origin == null && FiberServer.get().isDev()) {
            return true;
        }
        
        if (allowedOrigins.isEmpty()) {
            return false;
        }
        return allowedOrigins.contains("*") || allowedOrigins.contains(origin);
    }

    public void handlePreflightRequest(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        String requestMethod = request.getHeader("Access-Control-Request-Method");
        String requestHeaders = request.getHeader("Access-Control-Request-Headers");

        if ((origin != null || FiberServer.get().isDev()) && requestMethod != null && isOriginAllowed(origin)) {
            configureCorsHeaders(request, response);
            
            // Pour les requêtes OPTIONS, on renvoie 200 OK
            response.setStatus(HttpServletResponse.SC_OK);
            System.out.println("Preflight request handled successfully");
        } else {
            System.out.println("Preflight request failed validation");
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
            if (origin == null) System.out.println("Origin is null");
            if (requestMethod == null) System.out.println("Request method is null");
            if (!isOriginAllowed(origin)) System.out.println("Origin not allowed");
        }
    }

    public void configureCorsHeaders(HttpServletRequest request, HttpServletResponse response) {
        String origin = request.getHeader("Origin");
        if ((origin != null || FiberServer.get().isDev()) && isOriginAllowed(origin)) {
            // Si credentials sont autorisés, on doit spécifier l'origine exacte
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
            
            // Ajout de Vary: Origin pour le cache
            response.addHeader("Vary", "Origin");
        } else {
            // Si l'origine n'est pas autorisée, on ne configure pas les en-têtes CORS
            response.setStatus(HttpServletResponse.SC_FORBIDDEN);
        }
    }
} 