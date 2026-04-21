package sh.fyz.fiber.core.authentication.oauth2;

import sh.fyz.fiber.core.authentication.entities.UserAuth;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import sh.fyz.fiber.util.FiberObjectMapper;

import java.net.InetAddress;
import java.net.URI;
import java.net.URLEncoder;
import java.net.URLDecoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
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
    private static final ObjectMapper OBJECT_MAPPER = new FiberObjectMapper();
    private static final Duration HTTP_TIMEOUT = Duration.ofSeconds(10);

    protected AbstractOAuth2Provider(String clientId, String clientSecret,
                                   String authorizationEndpoint, String tokenEndpoint,
                                   String userInfoEndpoint, String defaultScope) {
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(HTTP_TIMEOUT)
                .build();
        this.clientId = clientId;
        this.clientSecret = clientSecret;
        this.authorizationEndpoint = authorizationEndpoint;
        this.tokenEndpoint = tokenEndpoint;
        this.userInfoEndpoint = userInfoEndpoint;
        this.defaultScope = defaultScope;

        validateEndpointUrl(tokenEndpoint, "tokenEndpoint");
        validateEndpointUrl(userInfoEndpoint, "userInfoEndpoint");
    }

    private static void validateEndpointUrl(String url, String name) {
        try {
            URI uri = URI.create(url);
            String scheme = uri.getScheme();
            if (scheme == null || (!scheme.equals("https") && !scheme.equals("http"))) {
                throw new IllegalArgumentException("OAuth2 " + name + " must use HTTP(S): " + url);
            }
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("OAuth2 " + name + " must have a host: " + url);
            }
            // Resolve and check ALL addresses (multi-record DNS) — guard against rebinding.
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress addr : addrs) {
                validateResolvedAddress(addr, name, url);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Invalid OAuth2 " + name + " URL: " + url, e);
        }
    }

    /**
     * Reject any address that is loopback, link-local, site-local (RFC 1918), unique
     * local (IPv6 ULA fc00::/7), multicast, or any-local. Performed at <b>each</b>
     * request to defeat DNS rebinding.
     */
    public static void validateResolvedAddress(InetAddress addr, String name, String url) {
        if (addr.isLoopbackAddress()
                || addr.isAnyLocalAddress()
                || addr.isLinkLocalAddress()
                || addr.isSiteLocalAddress()
                || addr.isMulticastAddress()) {
            throw new IllegalArgumentException(
                    "OAuth2 " + name + " resolves to a forbidden address (" + addr.getHostAddress() + "): " + url);
        }
        // IPv6 unique-local fc00::/7 — InetAddress lacks a flag for it.
        byte[] bytes = addr.getAddress();
        if (bytes.length == 16 && (bytes[0] & 0xFE) == 0xFC) {
            throw new IllegalArgumentException(
                    "OAuth2 " + name + " resolves to an IPv6 ULA address (" + addr.getHostAddress() + "): " + url);
        }
    }

    /** Re-resolve and re-validate the host of {@code url} (called at each request). */
    private static void revalidateEndpointAtRequest(String url, String name) {
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null) {
                throw new IllegalArgumentException("OAuth2 " + name + " is missing host: " + url);
            }
            InetAddress[] addrs = InetAddress.getAllByName(host);
            for (InetAddress addr : addrs) {
                validateResolvedAddress(addr, name, url);
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to resolve OAuth2 " + name + ": " + url, e);
        }
    }

    protected String buildAuthorizationUrl(String state, String redirectUri, String scope) {
        try {
            Map<String, String> params = new HashMap<>();
            params.put("client_id", clientId);
            params.put("redirect_uri", redirectUri);
            params.put("response_type", "code");
            params.put("scope", scope);
            params.put("state", state);

            customizeAuthorizationParams(params);

            StringBuilder url = new StringBuilder(authorizationEndpoint);
            url.append("?");

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

    protected void customizeAuthorizationParams(Map<String, String> params) {
    }

    @Override
    public OAuth2CallbackResult processCallback(String code, String redirectUri) {
        try {
            OAuth2TokenResponse tokens = exchangeCodeForToken(code, redirectUri);
            Map<String, Object> userInfo = getUserInfoFromToken(tokens.getAccessToken());
            return new OAuth2CallbackResult(userInfo, tokens);
        } catch (Exception e) {
            throw new RuntimeException("Failed to process OAuth callback", e);
        }
    }

    protected OAuth2TokenResponse exchangeCodeForToken(String code, String redirectUri) {
        revalidateEndpointAtRequest(tokenEndpoint, "tokenEndpoint");
        try {
            String requestBody = String.format("grant_type=authorization_code&code=%s&redirect_uri=%s",
                URLEncoder.encode(code, StandardCharsets.UTF_8),
                URLEncoder.encode(redirectUri, StandardCharsets.UTF_8));
            return postTokenRequest(requestBody);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to exchange code for token", e);
        }
    }

    @Override
    public OAuth2TokenResponse refreshAccessToken(String refreshToken) {
        if (refreshToken == null || refreshToken.isBlank()) {
            return null;
        }
        revalidateEndpointAtRequest(tokenEndpoint, "tokenEndpoint");
        try {
            String requestBody = String.format("grant_type=refresh_token&refresh_token=%s",
                URLEncoder.encode(refreshToken, StandardCharsets.UTF_8));
            return postTokenRequest(requestBody);
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new RuntimeException("Failed to refresh access token", e);
        }
    }

    /** Shared POST against the token endpoint used by code exchange and refresh. */
    private OAuth2TokenResponse postTokenRequest(String requestBody) throws Exception {
        String credentials = Base64.getEncoder().encodeToString(
            (clientId + ":" + clientSecret).getBytes(StandardCharsets.UTF_8));

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(tokenEndpoint))
            .header("Authorization", "Basic " + credentials)
            .header("Content-Type", "application/x-www-form-urlencoded")
            .header("Accept", "application/json")
            .timeout(HTTP_TIMEOUT)
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() != 200) {
            throw new RuntimeException("Token endpoint error (" + response.statusCode() + "): " + response.body());
        }

        Map<String, Object> data = parseJsonResponseToMap(response.body());
        return toTokenResponse(data);
    }

    private static OAuth2TokenResponse toTokenResponse(Map<String, Object> data) {
        String accessToken = stringField(data, "access_token");
        String tokenType = stringField(data, "token_type");
        String refresh = stringField(data, "refresh_token");
        String scope = stringField(data, "scope");
        Long expiresIn = longField(data, "expires_in");
        return new OAuth2TokenResponse(accessToken, tokenType, expiresIn, refresh, scope);
    }

    private static String stringField(Map<String, Object> data, String key) {
        Object v = data.get(key);
        return v == null ? null : v.toString();
    }

    private static Long longField(Map<String, Object> data, String key) {
        Object v = data.get(key);
        if (v == null) return null;
        if (v instanceof Number n) return n.longValue();
        try {
            return Long.parseLong(v.toString());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    protected Map<String, Object> getUserInfoFromToken(String accessToken) {
        revalidateEndpointAtRequest(userInfoEndpoint, "userInfoEndpoint");
        try {
            HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(userInfoEndpoint))
                .header("Authorization", "Bearer " + accessToken)
                .timeout(HTTP_TIMEOUT)
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
        try {
            if (json != null && json.trim().startsWith("{")) {
                return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, String>>() {});
            }
            return parseFormUrlEncoded(json);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response", e);
        }
    }

    protected Map<String, Object> parseJsonResponseToMap(String json) {
        try {
            if (json != null && json.trim().startsWith("{")) {
                return OBJECT_MAPPER.readValue(json, new TypeReference<Map<String, Object>>() {});
            }
            Map<String, String> stringMap = parseFormUrlEncoded(json);
            return new HashMap<>(stringMap);
        } catch (Exception e) {
            throw new RuntimeException("Failed to parse JSON response to map", e);
        }
    }

    private Map<String, String> parseFormUrlEncoded(String body) {
        Map<String, String> map = new HashMap<>();
        if (body == null || body.isEmpty()) {
            return map;
        }
        for (String pair : body.split("&")) {
            if (pair.isEmpty()) {
                continue;
            }
            int idx = pair.indexOf('=');
            String rawKey = idx >= 0 ? pair.substring(0, idx) : pair;
            String rawValue = idx >= 0 ? pair.substring(idx + 1) : "";
            String key = URLDecoder.decode(rawKey, StandardCharsets.UTF_8);
            String value = URLDecoder.decode(rawValue, StandardCharsets.UTF_8);
            map.put(key, value);
        }
        return map;
    }
}
