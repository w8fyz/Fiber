package sh.fyz.fiber.core.authentication.oauth2.client.controller;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.params.Param;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.OAuth2Client;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ClientService;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

@Controller("/oauth/client")
public class OAuth2ClientController {
    private final OAuth2ClientService clientService;
    private final AuthenticationService<?> authService;

    public OAuth2ClientController(OAuth2ClientService clientService) {
        this.clientService = clientService;
        this.authService = FiberServer.get().getAuthService();
    }

    @RequestMapping(value = "/authorize", method = RequestMapping.Method.GET)
    public ResponseEntity<?> authorize(
            @Param("client_id") String clientId,
            @Param("redirect_uri") String redirectUri,
            @Param("response_type") String responseType,
            @Param(value = "state", required = false) String state,
            @AuthenticatedUser UserAuth user,
            HttpServletRequest request,
            HttpServletResponse response) {

        if (!clientService.validateRedirectUri(clientId, redirectUri)) {
            return ResponseEntity.badRequest("Invalid client or redirect URI");
        }

        if (!"code".equals(responseType)) {
            return ResponseEntity.badRequest("Unsupported response type");
        }

        String code = clientService.generateAuthorizationCode(user, clientId);

        String redirectUrl = redirectUri + "?code=" + URLEncoder.encode(code, StandardCharsets.UTF_8);
        if (state != null) {
            redirectUrl += "&state=" + URLEncoder.encode(state, StandardCharsets.UTF_8);
        }

        response.setHeader("Location", redirectUrl);
        response.setStatus(HttpServletResponse.SC_FOUND);
        return null;
    }

    @RequestMapping(value = "/token", method = RequestMapping.Method.POST)
    public ResponseEntity<?> token(@Param("code") String code, OAuth2ApplicationInfo applicationInfo) {

        OAuth2Client client = clientService.getClientByCredentials(applicationInfo.clientId(), applicationInfo.clientSecret());
        if (client == null || !client.isEnabled()) {
            return ResponseEntity.unauthorized("Invalid client credentials");
        }

        OAuth2ClientService.AuthorizationCode authCode = clientService.validateAuthorizationCode(code, applicationInfo.clientId());
        if (authCode == null) {
            return ResponseEntity.unauthorized("Invalid authorization code");
        }

        String accessToken = clientService.generateAccessToken(authCode.getUser(), applicationInfo.clientId());

        Map<String, String> responseBody = new HashMap<>();
        responseBody.put("access_token", accessToken);
        responseBody.put("token_type", "Bearer");
        responseBody.put("expires_in", "3600");

        return ResponseEntity.ok(responseBody);
    }
}
