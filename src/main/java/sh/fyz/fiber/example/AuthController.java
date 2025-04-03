package sh.fyz.fiber.example;

import jakarta.servlet.http.HttpServletRequest;
import sh.fyz.fiber.FiberServer;
import sh.fyz.fiber.annotations.*;
import sh.fyz.fiber.core.AuthenticationService;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.UserAuth;

import java.util.HashMap;
import java.util.Map;

@Controller("/auth")
public class AuthController {
    
    public AuthController() {
    }

    @RequestMapping(value = "/register", method = RequestMapping.Method.POST)
    public ResponseEntity<String> register(@RequestBody User user) {
        Main.userRepository.save(user);
        return ResponseEntity.ok("User registered successfully");
    }

    @RequestMapping(value = "/login", method = RequestMapping.Method.POST)
    public ResponseEntity<String> login(@Param("value") String value, @Param("password") String password, HttpServletRequest request) {
        UserAuth user = FiberServer.get().getAuthService().findUserByIdentifer(value);
        if (user != null && FiberServer.get().getAuthService().validateCredentials(user.getId(), password)) {
            return ResponseEntity.ok(FiberServer.get().getAuthService().generateToken(user, request));
        }
        return ResponseEntity.unauthorized("Invalid credentials");
    }

    @RequestMapping(value = "/me", method = RequestMapping.Method.GET)
    public ResponseEntity<UserAuth> getCurrentUser(@AuthenticatedUser UserAuth user) {
        return ResponseEntity.ok(user);
    }
} 