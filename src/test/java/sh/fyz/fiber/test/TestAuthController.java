package sh.fyz.fiber.test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.params.AuthenticatedUser;
import sh.fyz.fiber.annotations.params.RequestBody;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.NoCSRF;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.AuthenticationService;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.authentication.entities.UserFieldUtil;

import java.util.Map;

@Controller("/test-auth")
public class TestAuthController {

    private final TestAuthService authService;

    public TestAuthController(TestAuthService authService) {
        this.authService = authService;
    }

    @RequestMapping(value = "/register", method = RequestMapping.Method.POST)
    @NoCSRF
    public ResponseEntity<Object> register(@RequestBody TestUser user,
                                            HttpServletRequest request,
                                            HttpServletResponse response) {
        if (FiberServer.get().getAuthService().doesIdentifiersAlreadyExists(user)) {
            return ResponseEntity.badRequest(Map.of("error", "User already exists"));
        }
        TestUser creating = new TestUser();
        creating.setUsername(user.getUsername());
        creating.setEmail(user.getEmail());
        creating.setRole(user.getRole() != null ? user.getRole() : "user");
        UserFieldUtil.setPassword(creating, user.getPassword());

        authService.saveUser(creating);
        authService.setAuthCookies(creating, request, response);
        return ResponseEntity.created(Map.of("id", creating.getId(), "username", creating.getUsername()));
    }

    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    @NoCSRF
    public ResponseEntity<Object> login(@RequestBody Map<String, String> body,
                                         HttpServletRequest request,
                                         HttpServletResponse response) {
        String identifier = body.getOrDefault("identifier", "");
        String password = body.getOrDefault("password", "");

        AuthenticationService<?> service = FiberServer.get().getAuthService();
        UserAuth user = service.findUserByIdentifer(identifier);
        if (user == null || !service.validateCredentials(user, password)) {
            return ResponseEntity.unauthorized(Map.of("error", "Invalid credentials"));
        }

        service.setAuthCookies(user, request, response);
        return ResponseEntity.ok(Map.of("id", user.getId(), "role", user.getRole()));
    }

    @RequestMapping(value = "/logout", method = RequestMapping.Method.POST)
    @NoCSRF
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    public ResponseEntity<Object> logout(@AuthenticatedUser UserAuth user,
                                          HttpServletRequest request,
                                          HttpServletResponse response) {
        FiberServer.get().getAuthService().clearAuthCookies(request, response);
        return ResponseEntity.ok(Map.of("status", "logged_out"));
    }
}
