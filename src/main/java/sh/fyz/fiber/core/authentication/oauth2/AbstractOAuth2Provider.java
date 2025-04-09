package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Abstract base class for OAuth2 providers with common functionality.
 * 
 * @param <T> The type of user entity used in the application
 */
public abstract class AbstractOAuth2Provider<T extends UserAuth> implements OAuth2Provider<T> {
    protected final HttpClient httpClient;
    protected final String clientId;
    protected final String clientSecret;
    protected final String authorizationEndpoint;
    protected final String tokenEndpoint;
    protected final String userInfoEndpoint;
    protected final String defaultScope;

    protected AbstractOAuth2Provider(String clientId, String clientSecret, 
                                   String authorizationEndpoint, String tokenEndpoint, 
                                   String userInfoEndpoint, String defaultScope) {
        this.httpClient = HttpClient.newHttpClient();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
        this.defaultScope = defaultScope;
    }

    protected String buildAuthorizationUrl(String state, String redirectUri, String scope) {
        try {
            // Create a map of parameters that can be customized by subclasses
            Map<String, String> params = new HashMap<>();
            params.put("client_id", clientId);
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");
            params.put("scope", scope);
            params.put("state", state);
            
            // Allow subclasses to add or modify parameters
            customizeAuthorizationParams(params);
            
            // Build the URL with all parameters
            StringBuilder url = new StringBuilder(authorizationEndpoint);
            url.append("?");
            
            // Add all parameters to the URL
            String paramString = params.entrySet().stream()
                .map(entry -> {
                    try {
                        return URLEncoder.encode(entry.getKey(), StandardCharsets.UTF_8) + "=" +
                               URLEncoder.encode(entry.getValue(), StandardCharsets.UTF_8);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to encode parameter", e);
                    }
                })
                .collect(Collectors.joining("&"));
            
            url.append(paramString);
            return url.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }
    
    /**
     * Allow subclasses to customize the authorization parameters.
     * This method can be overridden to add provider-specific parameters or modify existing ones.
     * 
     * @param params The map of parameters to customize
     */
    protected void customizeAuthorizationParams(Map<String, String> params) {
        // Default implementation does nothing
        // Subclasses can override this to add or modify parameters
    }

    @Override
    public Map<String, Object> processCallback(String code, String redirectUri) {
        try {
            // Exchange code for token
            String accessToken = exchangeCodeForToken(code, redirectUri);
            
            // Get user info using the access token
            return getUserInfoFromToken(accessToken);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process OAuth callback", e);
        }
    }

    protected String exchangeCodeForToken(String code, String redirectUri) {
        try {
            String credentials = Base64.getEncoder().encodeToString(
                (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

            String requestBody = String.format("grant_type=authorization_code&code=%s&redirect_uri=%s",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));

            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(tokenEndpoint))
                .header("Authorization", "Basic " + credentials)
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(requestBody))
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to exchange code for token: " + response.body());
            }

            // Parse the JSON response
            Map<String, String> tokenData = parseJsonResponse(response.body());
            return tokenData.get("access_token");
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    protected Map<String, Object> getUserInfoFromToken(String accessToken) {
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(java.net.URI.create(userInfoEndpoint))
                .header("Authorization", "Bearer " + accessToken)
                .GET()
                .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());
            
            if (response.statusCode() != 200) {
                throw new RuntimeException("Failed to get user info: " + response.body());
            }

            return parseJsonResponseToMap(response.body());
        } catch (Exception e) {
            throw new RuntimeException("Failed to get user info", e);
        }
    }

    protected Map<String, String> parseJsonResponse(String json) {
        // This is a simplified version - you should use a proper JSON library
        // Remove the outer braces and split by commas
        String content = json.substring(1, json.length() - 1);
        return java.util.Arrays.stream(content.split(","))
            .map(pair -> pair.split(":"))
            .collect(Collectors.toMap(
                parts -> parts[0].trim().replaceAll("\"", ""),
                parts -> parts[1].trim().replaceAll("\"", "")
            ));
    }
    
    protected Map<String, Object> parseJsonResponseToMap(String json) {
        // This is a simplified version - you should use a proper JSON library
        Map<String, Object> result = new HashMap<>();
        String content = json.substring(1, json.length() - 1);
        
        for (String pair : content.split(",")) {
            String[] parts = pair.split(":");
            if (parts.length == 2) {
                String key = parts[0].trim().replaceAll("\"", "");
                String value = parts[1].trim().replaceAll("\"", "");
                result.put(key, value);
            }
        }
        
        return result;
    }
} 