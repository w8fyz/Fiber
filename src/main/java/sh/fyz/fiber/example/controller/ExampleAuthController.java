package sh.fyz.fiber.example.controller;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.params.Param;
import sh.fyz.fiber.annotations.params.PathVariable;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.challenge.Challenge;
import sh.fyz.fiber.core.challenge.ChallengeCallback;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;
import sh.fyz.fiber.example.ExampleMain;
import sh.fyz.fiber.example.ExampleOAuthService;
import sh.fyz.fiber.example.repo.entities.ExampleUser;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller("/auth")
public class ExampleAuthController {
    
    private final ExampleOAuthService oauthServiceExample;
    
    public ExampleAuthController(ExampleOAuthService oauthServiceExample) {
        this.oauthServiceExample = oauthServiceExample;
    }

    @RequestMapping(value = "/register", method = RequestMapping.Method.POST)
    @AuditLog(action = "USER_REGISTRATION", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<String> register(@RequestBody ExampleUser exampleUser) {
        ExampleUser creating = new ExampleUser();
        creating.setEmail(exampleUser.getEmail());
        creating.setUsername(exampleUser.getUsername());
        creating.setRole("user");
        UserFieldUtil.setPassword(creating, exampleUser.getPassword());
        boolean exist = FiberServer.get().getAuthService().doesIdentifiersAlreadyExists(exampleUser);
        if(exist) {
            return ResponseEntity.badRequest("User with this identifier already exists");
        }
        ExampleMain.exampleUserRepository.save(creating);
        return ResponseEntity.ok("User registered successfully");
    }

    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    @RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
    @AuditLog(action = "LOGIN_ATTEMPT", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<Object> login(
            @RequestBody Map<String, String> body,
            HttpServletRequest request,
            HttpServletResponse response) {

        String identifier = body.get("identifier") != null ? body.get("identifier") : "";
        String password = body.get("password") != null ? body.get("password") : "";
        
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        ExampleUser exampleUser = (ExampleUser) authService.findUserByIdentifer(identifier);

        
        return ResponseEntity.unauthorized("Invalid credentials");
    }

    @RequestMapping(value = "/oauth/{provider}/login", method = RequestMapping.Method.GET)
    @AuditLog(action = "OAUTH_LOGIN_START", logParameters = true)
    public void oauthLogin(
            @PathVariable("provider") String provider,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        OAuth2Provider<ExampleUser> oauthProvider = oauthServiceExample.getProvider(provider);
        if (oauthProvider != null) {
            String requestUrl = request.getRequestURL().toString();
            String callbackUrl = requestUrl.replace("/login", "/callback");
            String redirectUrl = oauthServiceExample.getAuthorizationUrl(provider, callbackUrl);
            response.setHeader("Location", redirectUrl);
            response.setStatus(HttpServletResponse.SC_FOUND);
        } else {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Provider not found: " + provider);
        }
    }

    @RequestMapping(value = "/oauth/{provider}/callback", method = RequestMapping.Method.GET)
    @AuditLog(action = "OAUTH_CALLBACK", logParameters = true)
    public ResponseEntity<Map<String, String>> oauthCallback(
            @PathVariable("provider") String provider,
            @Param("code") String code,
            @Param("state") String state,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        try {

        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication error: " + e.getMessage());
        }
        return null;
    }

    @RequestMapping(value = "/refresh", method = RequestMapping.Method.POST)
    @AuditLog(action = "TOKEN_REFRESH", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<Map<String, String>> refreshToken(
            HttpServletRequest request,
            HttpServletResponse response) {
        
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        String ipAddress = authService.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        // Get refresh token from cookie
        String refreshToken = null;
        Cookie[] cookies = request.getCookies();
        if (cookies != null) {
            for (Cookie cookie : cookies) {
                if ("refresh_token".equals(cookie.getName())) {
                    refreshToken = cookie.getValue();
                    break;
                }
            }
        }

        if (refreshToken == null) {
            return ResponseEntity.unauthorized(Map.of("error", "No refresh token provided"));
        }

        if (JwtUtil.validateRefreshToken(refreshToken, ipAddress, userAgent)) {
            Object userId = JwtUtil.extractId(refreshToken);
            UserAuth user = authService.getUserById(userId);
            
            if (user != null) {
                // Set new auth cookies
                authService.setAuthCookies(user, request, response);
                
                Map<String, String> tokens = new HashMap<>();
                tokens.put("token_type", "Cookie");
                tokens.put("expires_in", "3600");
                
                return ResponseEntity.ok(tokens);
            }
        }
        
        return ResponseEntity.unauthorized(Map.of("error", "Invalid refresh token"));
    }

    @RequestMapping(value = "/logout", method = RequestMapping.Method.POST)
    @AuditLog(action = "LOGOUT", logParameters = false)
    public ResponseEntity<String> logout(HttpServletResponse response, HttpServletRequest request) {
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        authService.clearAuthCookies(request, response);
        return ResponseEntity.ok("Logged out successfully");
    }

    @RequestMapping(value = "/me", method = RequestMapping.Method.GET)
    @AuditLog(action = "GET_CURRENT_USER", logParameters = false)
    public ResponseEntity<UserAuth> getCurrentUser(@AuthenticatedUser UserAuth user) {
        return ResponseEntity.ok(user);
    }
} 