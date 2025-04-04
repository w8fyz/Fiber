package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
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

    protected AbstractOAuth2Provider(String clientId, String clientSecret, 
                                   String authorizationEndpoint, String tokenEndpoint, 
                                   String userInfoEndpoint) {
        this.httpClient = HttpClient.newHttpClient();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
    }

    @Override
    public String getClientId() {
        return clientId;
    }

    @Override
    public String getClientSecret() {
        return clientSecret;
    }

    protected String buildAuthorizationUrl(String state, String redirectUri, String scope) {
        try {
            return String.format("%s?client_id=%s&redirect_uri=%s&response_type=code&scope=%s&state=%s",
                authorizationEndpoint,
                URLEncoder.encode(clientId, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8),
                URLEncoder.encode(scope, StandardCharsets.UTF_8),
                URLEncoder.encode(state, StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException("Failed to build authorization URL", e);
        }
    }

    protected OAuth2TokenResponse exchangeCodeForToken(String code, String redirectUri) {
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

            // Parse the JSON response and create OAuth2TokenResponse
            // This is a simplified version - you might want to use a JSON library
            Map<String, String> tokenData = parseJsonResponse(response.body());
            
            return new OAuth2TokenResponse(
                tokenData.get("access_token"),
                tokenData.get("token_type"),
                Long.parseLong(tokenData.get("expires_in")),
                tokenData.get("refresh_token"),
                tokenData.get("scope")
            );
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

            return parseJsonResponse(response.body());
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
} 