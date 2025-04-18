package sh.fyz.fiber.example;

import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.NoCors;
import sh.fyz.fiber.annotations.security.NoCSRF;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.oauth2.OAuth2ApplicationInfo;

import java.util.HashMap;
import java.util.Map;

@Controller("/test2")
public class Test2Controller {

    @RequestMapping("/public")
    public ResponseEntity<?> publicEndpoint() {
        return ResponseEntity.ok("Cet endpoint est public !");
    }

    @RequestMapping("/bearer-only")
    @AuthType(AuthScheme.BEARER)
    public ResponseEntity<?> bearerOnly(@AuthenticatedUser UserAuth user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentifié avec Bearer Token");
        response.put("userId", user.getId());
        return ResponseEntity.ok(response);
    }

    @RequestMapping("/cookie-only")
    public ResponseEntity<?> cookieOnly(@AuthenticatedUser UserAuth user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentifié avec Cookie (par défaut)");
        response.put("userId", user.getId());
        return ResponseEntity.ok(response);
    }

    @RequestMapping("/basic-auth")
    @AuthType(AuthScheme.BASIC)
    @NoCors
    @NoCSRF
    public ResponseEntity<?> basicAuth(OAuth2ApplicationInfo app) {
        return ResponseEntity.ok(app);
    }

    @RequestMapping("/multi-auth")
    @AuthType({AuthScheme.BEARER, AuthScheme.COOKIE})
    public ResponseEntity<?> multiAuth(@AuthenticatedUser UserAuth user) {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Authentifié avec Bearer Token ou Cookie");
        response.put("userId", user.getId());
        return ResponseEntity.ok(response);
    }
} 