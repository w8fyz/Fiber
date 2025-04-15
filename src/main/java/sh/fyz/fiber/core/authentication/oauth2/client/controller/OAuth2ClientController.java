package sh.fyz.fiber.core.authentication.oauth2.client.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.params.Param;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.AuthMiddleware;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Controller("/oauth/client")
public class OAuth2ClientController {
    private final OAuth2ClientService clientService;
    private final AuthenticationService<?> authService;

    public OAuth2ClientController(OAuth2ClientService clientService) {
        this.clientService = clientService;
        this.authService = FiberServer.get().getAuthService();
    }

    @RequestMapping(value = "/test", method = RequestMapping.Method.GET)
    public ResponseEntity<?> test() {
        return ResponseEntity.ok(clientService.registerClient("Test Client", "http://localhost:8080/callback"));
    }

    @RequestMapping(value = "/authorize", method = RequestMapping.Method.GET)
    public ResponseEntity<?> authorize(
            @Param("client_id") String clientId,
            @Param("redirect_uri") String redirectUri,
            @Param("response_type") String responseType,
            @Param("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) {
        
        // Validate client
        if (!clientService.validateRedirectUri(clientId, redirectUri)) {
            return ResponseEntity.badRequest("Invalid client or redirect URI");
        }

        // Validate response type
        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest("Unsupported response type");
        }

        // Check if user is authenticated
        UserAuth user = AuthMiddleware.getCurrentUser(request);
        if (user == null) {
            // Store the authorization request
            String requestId = UUID.randomUUID().toString();
            clientService.storePendingRequest(requestId, clientId, redirectUri, state);
            
            // Redirect to login page with return URL
            String loginUrl = "/auth/login?return_to=/oauth/authorize?" + request.getQueryString();
            response.setHeader("Location", loginUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
            return null;
        }

        // Generate authorization code
        String code = clientService.generateAuthorizationCode(user, clientId);
        
        // Build redirect URL with code
        String redirectUrl = redirectUri + "?code=" + code;
        if (state != null) {
            redirectUrl += "&state=" + state;
        }
        
        response.setHeader("Location", redirectUrl);
        response.setStatus(HttpServletResponse.SC_FOUND);
        return null;
    }

    @RequestMapping(value = "/token", method = RequestMapping.Method.POST)
    public ResponseEntity<?> token(
            @Param("grant_type") String grantType,
            @Param("code") String code,
            @Param("redirect_uri") String redirectUri,
            @Param("client_id") String clientId,
            @Param("client_secret") String clientSecret) {
        
        // Validate client credentials
        OAuth2Client client = clientService.getClientByCredentials(clientId, clientSecret);
        if (client == null || !client.isEnabled()) {
            return ResponseEntity.unauthorized("Invalid client credentials");
        }

        // Validate grant type
        if (!"authorization_code".equals(grantType)) {
            return ResponseEntity.badRequest("Unsupported grant type");
        }

        // Validate and exchange authorization code
        OAuth2ClientService.AuthorizationCode authCode = clientService.validateAuthorizationCode(code, clientId);
        if (authCode == null) {
            return ResponseEntity.unauthorized("Invalid authorization code");
        }

        // Generate access token
        String accessToken = clientService.generateAccessToken(authCode.getUser(), clientId);
        
        Map<String, String> response = new HashMap<>();
        response.put("access_token", accessToken);
        response.put("token_type", "Bearer");
        response.put("expires_in", "3600");
        
        return ResponseEntity.ok(response);
    }
} 