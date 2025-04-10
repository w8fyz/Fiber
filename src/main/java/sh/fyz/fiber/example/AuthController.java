package sh.fyz.fiber.example;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.*;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.JwtUtil;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2Provider;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller("/auth")
public class AuthController {
    
    private final OAuthService oauthService;
    
    public AuthController(OAuthService oauthService) {
        this.oauthService = oauthService;
    }

    @RequestMapping(value = "/register", method = RequestMapping.Method.POST)
    @AuditLog(action = "USER_REGISTRATION", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<String> register(@RequestBody User user) {
        Main.userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    @RateLimit(attempts = 5, timeout = 15, unit = TimeUnit.MINUTES)
    @AuditLog(action = "LOGIN_ATTEMPT", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<Map<String, String>> login(
            @Param("value") String value, 
            @Param("password") String password, 
            HttpServletRequest request,
            HttpServletResponse response) {
        
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        UserAuth user = authService.findUserByIdentifer(value);
        
        if (user != null && authService.validateCredentials(user.getId(), password)) {
            // Set auth cookies
            authService.setAuthCookies(user, request, response);
            
            Map<String, String> tokens = new HashMap<>();
            tokens.put("token_type", "Bearer");
            tokens.put("expires_in", "3600");
            
            return ResponseEntity.ok(tokens);
        }
        
        return ResponseEntity.unauthorized(Map.of("error", "Invalid credentials"));
    }

    @RequestMapping(value = "/oauth/{provider}/login", method = RequestMapping.Method.GET)
    @AuditLog(action = "OAUTH_LOGIN_START", logParameters = true)
    public void oauthLogin(
            @PathVariable("provider") String provider,
            HttpServletRequest request,
            HttpServletResponse response) throws IOException {
        OAuth2Provider<User> oauthProvider = oauthService.getProvider(provider);
        if (oauthProvider != null) {
            String requestUrl = request.getRequestURL().toString();
            String callbackUrl = requestUrl.replace("/login", "/callback");
            String redirectUrl = oauthService.getAuthorizationUrl(provider, callbackUrl);
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
            User user = oauthService.handleCallback(code, state, request.getRequestURL().toString(), request, response);
            if (user != null) {
                Map<String, String> tokens = new HashMap<>();
                tokens.put("token_type", "Bearer");
                tokens.put("expires_in", "3600");

                return ResponseEntity.ok(tokens);
            } else {
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, "OAuth authentication failed");
            }
        } catch (Exception e) {
            response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "OAuth authentication error: " + e.getMessage());
        }
        return null;
    }

    @RequestMapping(value = "/refresh", method = RequestMapping.Method.POST)
    @AuditLog(action = "TOKEN_REFRESH", logParameters = true, maskSensitiveData = true)
    public ResponseEntity<Map<String, String>> refreshToken(
            @Param("refresh_token") String refreshToken, 
            HttpServletRequest request,
            HttpServletResponse response) {
        
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        String ipAddress = authService.getClientIpAddress(request);
        String userAgent = request.getHeader("User-Agent");

        if (JwtUtil.validateRefreshToken(refreshToken, ipAddress, userAgent)) {
            String userId = JwtUtil.extractId(refreshToken);
            UserAuth user = authService.getUserById(userId);
            
            if (user != null) {
                // Set new auth cookies
                authService.setAuthCookies(user, request, response);
                
                Map<String, String> tokens = new HashMap<>();
                tokens.put("token_type", "Bearer");
                tokens.put("expires_in", "3600");
                
                return ResponseEntity.ok(tokens);
            }
        }
        
        return ResponseEntity.unauthorized(Map.of("error", "Invalid refresh token"));
    }

    @RequestMapping(value = "/logout", method = RequestMapping.Method.POST)
    @AuditLog(action = "LOGOUT", logParameters = false)
    public ResponseEntity<String> logout(HttpServletResponse response) {
        AuthenticationService<?> authService = FiberServer.get().getAuthService();
        authService.clearAuthCookies(response);
        return ResponseEntity.ok("Logged out successfully");
    }

    @RequestMapping(value = "/me", method = RequestMapping.Method.GET)
    @AuditLog(action = "GET_CURRENT_USER", logParameters = false)
    public ResponseEntity<UserAuth> getCurrentUser(@AuthenticatedUser UserAuth user) {
        return ResponseEntity.ok(user);
    }
} 