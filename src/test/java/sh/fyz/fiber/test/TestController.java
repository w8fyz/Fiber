package sh.fyz.fiber.test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import sh.fyz.fiber.annotations.params.*;
import sh.fyz.fiber.annotations.request.Controller;
import sh.fyz.fiber.annotations.request.RequestMapping;
import sh.fyz.fiber.annotations.security.AuthType;
import sh.fyz.fiber.annotations.security.NoCSRF;
import sh.fyz.fiber.core.ResponseEntity;
import sh.fyz.fiber.core.authentication.AuthScheme;
import sh.fyz.fiber.core.authentication.entities.UserAuth;
import sh.fyz.fiber.core.security.annotations.AuditLog;
import sh.fyz.fiber.core.security.annotations.RateLimit;
import sh.fyz.fiber.core.security.logging.AuditContext;
import sh.fyz.fiber.core.session.FiberSession;
import sh.fyz.fiber.validation.Min;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Controller("/test")
public class TestController {

    @RequestMapping(value = "/hello", method = RequestMapping.Method.GET)
    public Map<String, String> hello() {
        return Map.of("message", "hello");
    }

    @RequestMapping(value = "/echo", method = RequestMapping.Method.GET)
    public Map<String, Object> echo(@Param("name") String name,
                                     @Param(value = "age", required = false) Integer age) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", name);
        if (age != null) {
            result.put("age", age);
        }
        return result;
    }

    @RequestMapping(value = "/echo-validated", method = RequestMapping.Method.GET)
    public Map<String, Object> echoValidated(@Param("name") String name,
                                              @Param("age") @Min(0) int age) {
        return Map.of("name", name, "age", age);
    }

    @RequestMapping(value = "/body", method = RequestMapping.Method.POST)
    @NoCSRF
    public Map<String, Object> body(@RequestBody TestRequestBody requestBody) {
        return Map.of("username", requestBody.getUsername(), "email", requestBody.getEmail());
    }

    @RequestMapping(value = "/update", method = RequestMapping.Method.PUT)
    @NoCSRF
    public Map<String, String> update() {
        return Map.of("status", "updated");
    }

    @RequestMapping(value = "/delete/{id}", method = RequestMapping.Method.DELETE)
    @NoCSRF
    public Map<String, String> delete(@PathVariable("id") String id) {
        return Map.of("deleted", id);
    }

    @RequestMapping(value = "/users/{id}", method = RequestMapping.Method.GET)
    public Map<String, String> getUserById(@PathVariable("id") String id) {
        return Map.of("userId", id);
    }

    @RequestMapping(value = "/users/me", method = RequestMapping.Method.GET)
    public Map<String, String> getCurrentUser() {
        return Map.of("userId", "me");
    }

    @RequestMapping(value = "/protected", method = RequestMapping.Method.GET)
    @AuthType({AuthScheme.COOKIE})
    public Map<String, Object> protectedEndpoint(@AuthenticatedUser UserAuth user) {
        return Map.of("userId", user.getId(), "role", user.getRole());
    }

    @RequestMapping(value = "/bearer", method = RequestMapping.Method.GET)
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    public Map<String, Object> bearerEndpoint(@AuthenticatedUser UserAuth user) {
        return Map.of("userId", user.getId(), "role", user.getRole());
    }

    @RequestMapping(value = "/session", method = RequestMapping.Method.GET)
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    public Map<String, Object> sessionEndpoint(@CurrentSession FiberSession session) {
        Map<String, Object> result = new HashMap<>();
        result.put("sessionId", session.getSessionId());
        result.put("userId", session.getUserId());
        result.put("ipAddress", session.getIpAddress());
        return result;
    }

    @RequestMapping(value = "/sessions", method = RequestMapping.Method.GET)
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    public List<Map<String, Object>> sessionsEndpoint(@AuthenticatedUser UserAuth user) {
        return user.getSessions().stream().map(s -> {
            Map<String, Object> m = new HashMap<>();
            m.put("sessionId", s.getSessionId());
            m.put("userId", s.getUserId());
            m.put("active", s.isActive());
            return m;
        }).toList();
    }

    @RequestMapping(value = "/sessions/{id}/invalidate", method = RequestMapping.Method.POST)
    @NoCSRF
    @AuthType({AuthScheme.COOKIE, AuthScheme.BEARER})
    public Map<String, String> invalidateSession(@AuthenticatedUser UserAuth user,
                                                  @PathVariable("id") String sessionId) {
        user.invalidateSession(sessionId);
        return Map.of("status", "invalidated");
    }

    @RequestMapping(value = "/dto", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, Object>> dtoEndpoint() {
        TestDtoObject dto = new TestDtoObject();
        dto.setName("test");
        dto.setAge(25);
        dto.setSecret("hidden");
        return ResponseEntity.ok(dto.asDTO());
    }

    @RequestMapping(value = "/bytes", method = RequestMapping.Method.GET)
    public ResponseEntity<byte[]> bytesEndpoint() {
        byte[] png = new byte[]{(byte) 0x89, 0x50, 0x4E, 0x47};
        return ResponseEntity.ok(png).contentType("image/png");
    }

    @RequestMapping(value = "/null", method = RequestMapping.Method.GET)
    public Object nullEndpoint() {
        return null;
    }

    @RequestMapping(value = "/rate-limited", method = RequestMapping.Method.GET)
    @RateLimit(attempts = 3, timeout = 1, unit = TimeUnit.MINUTES)
    public ResponseEntity<Map<String, String>> rateLimited() {
        return ResponseEntity.badRequest(Map.of("status", "fail"));
    }

    @RequestMapping(value = "/no-csrf", method = RequestMapping.Method.POST)
    @NoCSRF
    public Map<String, String> noCsrf() {
        return Map.of("csrf", "bypassed");
    }

    @RequestMapping(value = "/middleware-value", method = RequestMapping.Method.GET)
    public Map<String, Object> middlewareValue(HttpServletRequest request) {
        Object val = request.getAttribute("test-middleware");
        return Map.of("middlewareValue", val != null ? val : "none");
    }

    @RequestMapping(value = "/status/{code}", method = RequestMapping.Method.GET)
    public ResponseEntity<Map<String, String>> statusCode(@PathVariable("code") String code) {
        int status = Integer.parseInt(code);
        return ResponseEntity.ok(Map.of("code", code)).status(status);
    }

    @RequestMapping(value = "/audited", method = RequestMapping.Method.POST)
    @NoCSRF
    @AuditLog(action = "TEST_ACTION", logParameters = true, logResult = true, maskSensitiveData = true)
    public Map<String, Object> auditedEndpoint(@RequestBody Map<String, String> body) {
        AuditContext.put("customField", "customValue");
        AuditContext.put("itemCount", 42);
        return Map.of("status", "audited", "received", body);
    }

    @RequestMapping(value = "/audited-no-custom", method = RequestMapping.Method.GET)
    @AuditLog(action = "SIMPLE_READ", logParameters = false, logResult = false)
    public Map<String, String> auditedNoCustom() {
        return Map.of("status", "ok");
    }

    @RequestMapping(value = "/rate-limited-sliding", method = RequestMapping.Method.GET)
    @RateLimit(attempts = 3, timeout = 1, unit = TimeUnit.MINUTES, slidingWindow = true)
    public ResponseEntity<Map<String, String>> rateLimitedSliding() {
        return ResponseEntity.badRequest(Map.of("status", "fail"));
    }
}
